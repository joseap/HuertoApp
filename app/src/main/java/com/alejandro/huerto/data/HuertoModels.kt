package com.alejandro.huerto.data

import kotlin.math.roundToInt

data class ClimateSnapshot(
    val superiorTemperature: Double? = null,
    val superiorHumidity: Double? = null,
    val inferiorTemperature: Double? = null,
    val inferiorHumidity: Double? = null,
)

data class ValveState(
    val name: String,
    val isOn: Boolean = false,
    val lastChangedAt: Long? = null,
)

data class HomeStatus(
    val climate: ClimateSnapshot = ClimateSnapshot(),
    val soilSensors: List<SoilSensorLiveReading> = listOf(
        SoilSensorLiveReading(),
        SoilSensorLiveReading(),
        SoilSensorLiveReading(),
    ),
    val valves: List<ValveState> = listOf(
        ValveState(name = "V1"),
        ValveState(name = "V2"),
        ValveState(name = "V3"),
    ),
    val impulsePumpOn: Boolean = false,
    val fillPumpOn: Boolean = false,
    val filling: Boolean = false,
    val lowLevel: Boolean = false,
    val irrigationMode: String = "AUTO",
    val lastUpdatedAtMs: Long = 0L,
)

data class HomeFreshnessUiState(
    val message: String = "Esperando datos del sistema.",
    val isStale: Boolean = false,
    val isOffline: Boolean = false,
)

data class ActivityLogItem(
    val id: String,
    val type: String = "",
    val origin: String = "",
    val detail: String = "",
    val line: String? = null,
    val state: String? = null,
    val serverTimestamp: Long? = null,
)

data class ClimateHistorySample(
    val id: String,
    val timestamp: Long,
    val superiorTemperature: Double,
    val superiorHumidity: Double,
    val inferiorTemperature: Double,
    val inferiorHumidity: Double,
)

data class ClimateHistoryUiPoint(
    val timestamp: Long,
    val timeLabel: String,
    val superiorTemperature: Int,
    val superiorHumidity: Int,
    val inferiorTemperature: Int,
    val inferiorHumidity: Int,
)

data class SoilHistoryUiPoint(
    val timeLabel: String,
    val sensor1: Int,
    val sensor2: Int,
    val sensor3: Int,
)

data class ClimateSelectedDay(
    val year: Int,
    val month: Int,
    val day: Int,
)

data class HistorySeriesVisibility(
    val superiorTemperature: Boolean = true,
    val inferiorTemperature: Boolean = true,
    val superiorHumidity: Boolean = false,
    val inferiorHumidity: Boolean = false,
)

data class ManualControlUiState(
    val pendingValveIndex: Int? = null,
    val pendingOpenRequest: Boolean = false,
    val requestToken: Long = 0L,
    val message: String? = null,
    val isError: Boolean = false,
)

data class SensorCalibration(
    val min: Int = 0,
    val max: Int = 0,
)

data class SoilSensorLiveReading(
    val raw: Int? = null,
    val percentage: Int? = null,
)

data class HuertoConfig(
    val automaticIrrigationThresholdMinPercent: Int = 35,
    val automaticIrrigationThresholdMaxPercent: Int = 70,
    val readingIntervalMs: Int = 10_000,
    val intervaloLecturaClimaMs: Int = 300_000,
    val intervaloLecturaSueloMs: Int = 30_000,
    val sensor1: SensorCalibration = SensorCalibration(min = 26_000, max = 8_000),
    val sensor2: SensorCalibration = SensorCalibration(min = 26_500, max = 8_500),
    val sensor3: SensorCalibration = SensorCalibration(min = 27_000, max = 9_000),
)

data class DepositoConfig(
    val maxIrrigationMs: Int = 900_000,
    val refillRestartDelayMs: Int = 30_000,
)

data class SystemConfig(
    val huerto: HuertoConfig = HuertoConfig(),
    val deposito: DepositoConfig = DepositoConfig(),
)

data class ConfigEditorState(
    val automaticIrrigationThresholdMinPercent: String = "",
    val automaticIrrigationThresholdMaxPercent: String = "",
    val readingIntervalMs: String = "",
    val intervaloLecturaClimaMs: String = "",
    val intervaloLecturaSueloMs: String = "",
    val sensor1Min: String = "",
    val sensor1Max: String = "",
    val sensor2Min: String = "",
    val sensor2Max: String = "",
    val sensor3Min: String = "",
    val sensor3Max: String = "",
    val maxIrrigationMs: String = "",
    val refillRestartDelayMs: String = "",
)

data class ConfigUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val config: SystemConfig = SystemConfig(),
    val sensor1Live: SoilSensorLiveReading = SoilSensorLiveReading(),
    val sensor2Live: SoilSensorLiveReading = SoilSensorLiveReading(),
    val sensor3Live: SoilSensorLiveReading = SoilSensorLiveReading(),
    val editor: ConfigEditorState = ConfigEditorState(),
    val saveMessage: String? = null,
    val errorMessage: String? = null,
)

fun Double?.toRoundedIntOrNull(): Int? = this?.roundToInt()
