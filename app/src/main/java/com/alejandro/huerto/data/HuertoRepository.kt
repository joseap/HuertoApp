package com.alejandro.huerto.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HuertoRepository @Inject constructor(
    private val database: FirebaseDatabase,
) {
    private val root: DatabaseReference = database.reference.child("v1")

    fun observeHomeStatus(): Flow<HomeStatus> {
        val climateFlow = observeValue(root.child("telemetria").child("huerto")) { snapshot ->
            HuertoTelemetrySnapshot(
                climate = ClimateSnapshot(
                    superiorTemperature = snapshot.child("superior").child("temperatura").getValue(Double::class.java),
                    superiorHumidity = snapshot.child("superior").child("humedad").getValue(Double::class.java),
                    inferiorTemperature = snapshot.child("inferior").child("temperatura").getValue(Double::class.java),
                    inferiorHumidity = snapshot.child("inferior").child("humedad").getValue(Double::class.java),
                ),
                soilSensors = listOf(
                    parseSoilSensorReading(snapshot.child("humedadSuelo"), 1),
                    parseSoilSensorReading(snapshot.child("humedadSuelo"), 2),
                    parseSoilSensorReading(snapshot.child("humedadSuelo"), 3),
                ),
            )
        }

        val depositFlow = observeValue(root.child("telemetria").child("deposito")) { snapshot ->
            val valves = listOf(
                parseValve(snapshot, 1),
                parseValve(snapshot, 2),
                parseValve(snapshot, 3),
            )
            DepositSnapshot(
                valves = valves,
                impulsePumpOn = snapshot.child("bombas").child("impulsion").child("estado").getValue(String::class.java).equals("ON", ignoreCase = true),
                fillPumpOn = snapshot.child("bombas").child("llenado").child("estado").getValue(String::class.java).equals("ON", ignoreCase = true),
                filling = snapshot.child("llenando").getValue(Boolean::class.java) == true,
                lowLevel = snapshot.child("nivelBajo").getValue(Boolean::class.java) == true,
            )
        }

        val modeFlow = observeValue(root.child("estado").child("modoGlobal")) { snapshot ->
            snapshot.getValue(String::class.java) ?: "AUTO"
        }

        return combine(climateFlow, depositFlow, modeFlow) { climate, deposit, mode ->
            HomeStatus(
                climate = climate.climate,
                soilSensors = climate.soilSensors,
                valves = deposit.valves,
                impulsePumpOn = deposit.impulsePumpOn,
                fillPumpOn = deposit.fillPumpOn,
                filling = deposit.filling,
                lowLevel = deposit.lowLevel,
                irrigationMode = mode,
                lastUpdatedAtMs = System.currentTimeMillis(),
            )
        }
    }

    fun observeActivityLogs(limit: Int = 20): Flow<List<ActivityLogItem>> {
        val query = root.child("logs").child("eventos")
            .orderByChild("serverTimestamp")
            .limitToLast(limit)

        return observeValue(query) { snapshot ->
            snapshot.children.mapNotNull { child ->
                ActivityLogItem(
                    id = child.key.orEmpty(),
                    type = child.child("tipo").getValue(String::class.java).orEmpty(),
                    origin = child.child("origen").getValue(String::class.java).orEmpty(),
                    detail = child.child("detalle").getValue(String::class.java).orEmpty(),
                    line = child.child("linea").getValue(String::class.java),
                    state = child.child("estado").getValue(String::class.java),
                    serverTimestamp = child.child("serverTimestamp").getValue(Long::class.java),
                )
            }.sortedByDescending { it.serverTimestamp ?: 0L }.take(limit)
        }
    }

    fun observeClimateHistoryLast24Hours(limit: Int = 96): Flow<List<ClimateHistoryUiPoint>> {
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        val query = root.child("historico").child("clima").child("muestras")
            .orderByChild("ts")
            .startAt(cutoff.toDouble())
            .limitToLast(limit)

        return observeValue(query) { snapshot ->
            snapshot.children.mapNotNull { child ->
                val ts = child.child("ts").getValue(Long::class.java) ?: return@mapNotNull null
                if (ts < cutoff) return@mapNotNull null
                val superiorTemp = child.child("superior").child("temperatura").getValue(Double::class.java) ?: return@mapNotNull null
                val superiorHum = child.child("superior").child("humedad").getValue(Double::class.java) ?: return@mapNotNull null
                val inferiorTemp = child.child("inferior").child("temperatura").getValue(Double::class.java) ?: return@mapNotNull null
                val inferiorHum = child.child("inferior").child("humedad").getValue(Double::class.java) ?: return@mapNotNull null
                ClimateHistorySample(
                    id = child.key.orEmpty(),
                    timestamp = ts,
                    superiorTemperature = superiorTemp,
                    superiorHumidity = superiorHum,
                    inferiorTemperature = inferiorTemp,
                    inferiorHumidity = inferiorHum,
                )
            }.sortedByDescending { it.timestamp }
                .take(limit)
                .sortedBy { it.timestamp }
                .map { sample ->
                    ClimateHistoryUiPoint(
                        timestamp = sample.timestamp,
                        timeLabel = historyTimeFormatter.format(Date(sample.timestamp)),
                        superiorTemperature = sample.superiorTemperature.toRoundedIntOrNull() ?: 0,
                        superiorHumidity = sample.superiorHumidity.toRoundedIntOrNull() ?: 0,
                        inferiorTemperature = sample.inferiorTemperature.toRoundedIntOrNull() ?: 0,
                        inferiorHumidity = sample.inferiorHumidity.toRoundedIntOrNull() ?: 0,
                    )
                }
        }
    }

    fun observeClimateAggregateDay(selectedDay: Flow<ClimateSelectedDay>): Flow<List<ClimateHistoryUiPoint>> {
        return selectedDay.distinctUntilChanged().flatMapLatest { day ->
            observeValue(
                root.child("historico").child("clima").child("agregados").child("hora")
                    .child(day.year.toString())
                    .child(day.month.toString().padStart(2, '0'))
                    .child(day.day.toString().padStart(2, '0')),
            ) { snapshot ->
                normalizeClimateAggregateBuckets(
                    snapshot.children.map { bucket ->
                        ClimateAggregateBucketRaw(
                            bucketStartTs = bucket.child("bucketStartTs").getValue(Long::class.java),
                            superiorTemperatureAvg = bucket.child("superior").child("temperatura").child("avg").getValue(Double::class.java),
                            superiorHumidityAvg = bucket.child("superior").child("humedad").child("avg").getValue(Double::class.java),
                            inferiorTemperatureAvg = bucket.child("inferior").child("temperatura").child("avg").getValue(Double::class.java),
                            inferiorHumidityAvg = bucket.child("inferior").child("humedad").child("avg").getValue(Double::class.java),
                        )
                    },
                    historyTimeFormatter,
                )
            }
        }
    }

    fun observeSoilHistoryLast24Hours(limit: Int = 288): Flow<List<SoilHistoryUiPoint>> {
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        val query = root.child("historico").child("suelo").child("muestras")
            .orderByChild("ts")
            .startAt(cutoff.toDouble())
            .limitToLast(limit)

        return observeValue(query) { snapshot ->
            snapshot.children.mapNotNull { child ->
                val ts = child.child("ts").getValue(Long::class.java) ?: return@mapNotNull null
                if (ts < cutoff) return@mapNotNull null
                val s1 = child.child("sensor1").getValue(Int::class.java) ?: return@mapNotNull null
                val s2 = child.child("sensor2").getValue(Int::class.java) ?: return@mapNotNull null
                val s3 = child.child("sensor3").getValue(Int::class.java) ?: return@mapNotNull null
                SoilHistoryUiPoint(
                    timeLabel = historyTimeFormatter.format(Date(ts)),
                    sensor1 = s1,
                    sensor2 = s2,
                    sensor3 = s3,
                )
            }.sortedBy { it.timeLabel }
        }
    }

    fun observeSystemConfig(): Flow<SystemConfig> {
        val huertoFlow = observeValue(root.child("config").child("huerto")) { snapshot ->
            HuertoConfig(
                automaticIrrigationThresholdMinPercent = snapshot.child("umbrales").child("min").getValue(Int::class.java) ?: 35,
                automaticIrrigationThresholdMaxPercent = snapshot.child("umbrales").child("max").getValue(Int::class.java) ?: 70,
                readingIntervalMs = snapshot.child("intervaloLecturaMs").getValue(Int::class.java) ?: 10_000,
                intervaloLecturaClimaMs = snapshot.child("intervaloLecturaClimaMs").getValue(Int::class.java) ?: 300_000,
                intervaloLecturaSueloMs = snapshot.child("intervaloLecturaSueloMs").getValue(Int::class.java) ?: 30_000,
                sensor1 = parseSensorCalibration(snapshot, "sensor1", 26_000, 8_000),
                sensor2 = parseSensorCalibration(snapshot, "sensor2", 26_500, 8_500),
                sensor3 = parseSensorCalibration(snapshot, "sensor3", 27_000, 9_000),
            )
        }

        val depositoFlow = observeValue(root.child("config").child("deposito")) { snapshot ->
            DepositoConfig(
                maxIrrigationMs = snapshot.child("maximoRiegoMs").getValue(Int::class.java) ?: 900_000,
                refillRestartDelayMs = snapshot.child("delayReencendidoLlenadoMs").getValue(Int::class.java) ?: 30_000,
            )
        }

        return combine(huertoFlow, depositoFlow) { huerto, deposito ->
            SystemConfig(huerto = huerto, deposito = deposito)
        }
    }

    fun observeSoilSensorReadings(): Flow<List<SoilSensorLiveReading>> {
        return observeValue(root.child("telemetria").child("huerto").child("humedadSuelo")) { snapshot ->
            listOf(
                parseSoilSensorReading(snapshot, 1),
                parseSoilSensorReading(snapshot, 2),
                parseSoilSensorReading(snapshot, 3),
            )
        }
    }

    suspend fun saveSystemConfig(config: SystemConfig) {
        val updates = mapOf<String, Any>(
            "config/huerto/umbrales/min" to config.huerto.automaticIrrigationThresholdMinPercent,
            "config/huerto/umbrales/max" to config.huerto.automaticIrrigationThresholdMaxPercent,
            "config/huerto/intervaloLecturaMs" to config.huerto.readingIntervalMs,
            "config/huerto/intervaloLecturaClimaMs" to config.huerto.intervaloLecturaClimaMs,
            "config/huerto/intervaloLecturaSueloMs" to config.huerto.intervaloLecturaSueloMs,
            "config/huerto/calibracionHumedad/sensor1/min" to config.huerto.sensor1.min,
            "config/huerto/calibracionHumedad/sensor1/max" to config.huerto.sensor1.max,
            "config/huerto/calibracionHumedad/sensor2/min" to config.huerto.sensor2.min,
            "config/huerto/calibracionHumedad/sensor2/max" to config.huerto.sensor2.max,
            "config/huerto/calibracionHumedad/sensor3/min" to config.huerto.sensor3.min,
            "config/huerto/calibracionHumedad/sensor3/max" to config.huerto.sensor3.max,
            "config/deposito/maximoRiegoMs" to config.deposito.maxIrrigationMs,
            "config/deposito/delayReencendidoLlenadoMs" to config.deposito.refillRestartDelayMs,
        )
        root.updateChildren(updates).await()
    }

    suspend fun setValveManual(valveIndex: Int, shouldOpen: Boolean) {
        val timestamp = mapOf(".sv" to "timestamp")
        val updates = mapOf<String, Any>(
            "comandos/modoGlobal/solicitado" to "MANUAL",
            "comandos/modoGlobal/origen" to "android_app",
            "comandos/modoGlobal/timestamp" to timestamp,
            "comandos/valvulas/v$valveIndex/estadoSolicitado" to if (shouldOpen) "ON" else "OFF",
            "comandos/valvulas/v$valveIndex/origen" to "android_app",
            "comandos/valvulas/v$valveIndex/timestamp" to timestamp,
        )
        root.updateChildren(updates).await()
    }

    private fun parseValve(snapshot: DataSnapshot, valveIndex: Int): ValveState {
        val valveSnapshot = snapshot.child("valvulas").child("v$valveIndex")
        return ValveState(
            name = "V$valveIndex",
            isOn = valveSnapshot.child("estado").getValue(String::class.java).equals("ON", ignoreCase = true),
            lastChangedAt = valveSnapshot.child("ultimoCambio").getValue(Long::class.java),
        )
    }

    private fun parseSensorCalibration(
        snapshot: DataSnapshot,
        sensorKey: String,
        defaultMin: Int,
        defaultMax: Int,
    ): SensorCalibration {
        val calibrationSnapshot = snapshot.child("calibracionHumedad").child(sensorKey)
        return SensorCalibration(
            min = calibrationSnapshot.child("min").getValue(Int::class.java) ?: defaultMin,
            max = calibrationSnapshot.child("max").getValue(Int::class.java) ?: defaultMax,
        )
    }

    private fun parseSoilSensorReading(snapshot: DataSnapshot, sensorIndex: Int): SoilSensorLiveReading {
        val sensorSnapshot = snapshot.child("sensor$sensorIndex")
        return SoilSensorLiveReading(
            raw = sensorSnapshot.child("raw").getValue(Int::class.java),
            percentage = sensorSnapshot.child("porcentaje").getValue(Int::class.java),
        )
    }

    private fun <T> observeValue(query: Query, mapper: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(mapper(snapshot))
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) = Unit
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    private data class DepositSnapshot(
        val valves: List<ValveState>,
        val impulsePumpOn: Boolean,
        val fillPumpOn: Boolean,
        val filling: Boolean,
        val lowLevel: Boolean,
    )

    private data class HuertoTelemetrySnapshot(
        val climate: ClimateSnapshot,
        val soilSensors: List<SoilSensorLiveReading>,
    )

    private companion object {
        val historyTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }
}
