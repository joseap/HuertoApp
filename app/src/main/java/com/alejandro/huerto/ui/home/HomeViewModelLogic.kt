package com.alejandro.huerto.ui.home

import com.alejandro.huerto.data.ClimateSelectedDay
import com.alejandro.huerto.data.ConfigEditorState
import com.alejandro.huerto.data.DepositoConfig
import com.alejandro.huerto.data.HomeFreshnessUiState
import com.alejandro.huerto.data.HomeStatus
import com.alejandro.huerto.data.HuertoConfig
import com.alejandro.huerto.data.ManualControlUiState
import com.alejandro.huerto.data.SensorCalibration
import com.alejandro.huerto.data.SystemConfig
import java.util.Calendar

internal sealed interface ConfigValidationResult {
    data class Success(val config: SystemConfig) : ConfigValidationResult
    data class Error(val message: String) : ConfigValidationResult
}

internal fun ConfigEditorState.toSystemConfigOrError(): ConfigValidationResult {
    val irrigationMinValue = automaticIrrigationThresholdMinPercent.toIntOrNull()
        ?: return ConfigValidationResult.Error("El umbral mínimo debe ser un número entero.")
    val irrigationMaxValue = automaticIrrigationThresholdMaxPercent.toIntOrNull()
        ?: return ConfigValidationResult.Error("El umbral máximo debe ser un número entero.")
    val readingIntervalValue = readingIntervalMs.toIntOrNull()
        ?: return ConfigValidationResult.Error("El intervalo de lectura debe ser un número entero en milisegundos.")
    val climaIntervalValue = intervaloLecturaClimaMs.toIntOrNull()
        ?: return ConfigValidationResult.Error("El intervalo de clima debe ser un número entero en milisegundos.")
    val sueloIntervalValue = intervaloLecturaSueloMs.toIntOrNull()
        ?: return ConfigValidationResult.Error("El intervalo de suelo debe ser un número entero en milisegundos.")
    val sensor1MinValue = sensor1Min.toIntOrNull()
        ?: return ConfigValidationResult.Error("La calibración mínima del sensor 1 debe ser un entero.")
    val sensor1MaxValue = sensor1Max.toIntOrNull()
        ?: return ConfigValidationResult.Error("La calibración máxima del sensor 1 debe ser un entero.")
    val sensor2MinValue = sensor2Min.toIntOrNull()
        ?: return ConfigValidationResult.Error("La calibración mínima del sensor 2 debe ser un entero.")
    val sensor2MaxValue = sensor2Max.toIntOrNull()
        ?: return ConfigValidationResult.Error("La calibración máxima del sensor 2 debe ser un entero.")
    val sensor3MinValue = sensor3Min.toIntOrNull()
        ?: return ConfigValidationResult.Error("La calibración mínima del sensor 3 debe ser un entero.")
    val sensor3MaxValue = sensor3Max.toIntOrNull()
        ?: return ConfigValidationResult.Error("La calibración máxima del sensor 3 debe ser un entero.")
    val maxIrrigationValue = maxIrrigationMs.toIntOrNull()
        ?: return ConfigValidationResult.Error("El máximo de riego debe ser un número entero en milisegundos.")
    val refillDelayValue = refillRestartDelayMs.toIntOrNull()
        ?: return ConfigValidationResult.Error("El retardo de reencendido debe ser un número entero en milisegundos.")

    if (irrigationMinValue !in 0..100) {
        return ConfigValidationResult.Error("El umbral mínimo debe estar entre 0% y 100%.")
    }
    if (irrigationMaxValue !in 0..100) {
        return ConfigValidationResult.Error("El umbral máximo debe estar entre 0% y 100%.")
    }
    if (irrigationMinValue >= irrigationMaxValue) {
        return ConfigValidationResult.Error("El umbral mínimo debe ser menor que el máximo para que la lógica automática tenga histéresis.")
    }
    if (readingIntervalValue !in 1_000..600_000) {
        return ConfigValidationResult.Error("El intervalo de lectura debe quedar entre 1 s y 10 min.")
    }
    if (climaIntervalValue !in 30_000..1_800_000) {
        return ConfigValidationResult.Error("El intervalo de clima debe quedar entre 30 s y 30 min.")
    }
    if (sueloIntervalValue !in 5_000..300_000) {
        return ConfigValidationResult.Error("El intervalo de suelo debe quedar entre 5 s y 5 min.")
    }
    if (maxIrrigationValue !in 60_000..14_400_000) {
        return ConfigValidationResult.Error("El máximo de riego debe quedar entre 1 min y 4 h.")
    }
    if (refillDelayValue !in 5_000..3_600_000) {
        return ConfigValidationResult.Error("El retardo de reencendido debe quedar entre 5 s y 1 h.")
    }

    val sensorValidationError = validateSensorCalibration("sensor 1", sensor1MinValue, sensor1MaxValue)
        ?: validateSensorCalibration("sensor 2", sensor2MinValue, sensor2MaxValue)
        ?: validateSensorCalibration("sensor 3", sensor3MinValue, sensor3MaxValue)
    if (sensorValidationError != null) {
        return ConfigValidationResult.Error(sensorValidationError)
    }

    return ConfigValidationResult.Success(
        SystemConfig(
            huerto = HuertoConfig(
                automaticIrrigationThresholdMinPercent = irrigationMinValue,
                automaticIrrigationThresholdMaxPercent = irrigationMaxValue,
                readingIntervalMs = readingIntervalValue,
                intervaloLecturaClimaMs = climaIntervalValue,
                intervaloLecturaSueloMs = sueloIntervalValue,
                sensor1 = SensorCalibration(sensor1MinValue, sensor1MaxValue),
                sensor2 = SensorCalibration(sensor2MinValue, sensor2MaxValue),
                sensor3 = SensorCalibration(sensor3MinValue, sensor3MaxValue),
            ),
            deposito = DepositoConfig(
                maxIrrigationMs = maxIrrigationValue,
                refillRestartDelayMs = refillDelayValue,
            ),
        ),
    )
}

internal fun currentClimateSelectedDay(dayOffset: Int = 0): ClimateSelectedDay {
    val calendar = Calendar.getInstance()
    if (dayOffset != 0) {
        calendar.add(Calendar.DAY_OF_MONTH, dayOffset)
    }
    return ClimateSelectedDay(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
    )
}

internal fun validateSensorCalibration(sensorLabel: String, minValue: Int, maxValue: Int): String? {
    if (minValue <= maxValue) {
        return "La calibración del $sensorLabel debe cumplir min > max para representar seco > húmedo."
    }
    if ((minValue - maxValue) < 500) {
        return "La calibración del $sensorLabel tiene un margen demasiado pequeño. Amplía la diferencia entre min y max."
    }
    if (minValue <= 0 || maxValue <= 0) {
        return "La calibración del $sensorLabel debe usar valores raw positivos."
    }
    return null
}

internal fun HomeStatus.toFreshness(nowMs: Long): HomeFreshnessUiState {
    if (lastUpdatedAtMs <= 0L) {
        return HomeFreshnessUiState(
            message = "Esperando telemetría inicial del sistema.",
            isStale = true,
            isOffline = false,
        )
    }

    val ageMs = (nowMs - lastUpdatedAtMs).coerceAtLeast(0L)
    val ageSeconds = ageMs / 1_000L
    return when {
        ageMs < 15_000L -> HomeFreshnessUiState(
            message = "Actualizado hace ${formatElapsed(ageSeconds)}.",
            isStale = false,
            isOffline = false,
        )

        ageMs < 60_000L -> HomeFreshnessUiState(
            message = "Sin datos recientes. Última actualización hace ${formatElapsed(ageSeconds)}.",
            isStale = true,
            isOffline = false,
        )

        else -> HomeFreshnessUiState(
            message = "Mostrando último estado conocido. Sin actualización desde hace ${formatElapsed(ageSeconds)}.",
            isStale = true,
            isOffline = true,
        )
    }
}

internal fun resolveManualRequestConfirmation(
    currentState: ManualControlUiState,
    homeStatus: HomeStatus,
): ManualControlUiState? {
    val pending = currentState.pendingValveIndex ?: return null
    if (homeStatus.irrigationMode != "MANUAL") return null
    val requestedValve = homeStatus.valves.getOrNull(pending - 1) ?: return null
    if (currentState.pendingOpenRequest && requestedValve.isOn) {
        return ManualControlUiState(
            message = "V$pending confirmada en telemetría. Control manual activo.",
            isError = false,
        )
    }
    return null
}

internal fun unresolvedManualRequestMessage(lowLevel: Boolean): String {
    return if (lowLevel) {
        "La orden manual no se confirmó. El depósito está en nivel bajo y el riego queda bloqueado."
    } else {
        "La orden manual no se confirmó todavía en telemetría. Revisa conectividad o estado del depósito."
    }
}

internal fun formatElapsed(ageSeconds: Long): String = when {
    ageSeconds < 60L -> "${ageSeconds}s"
    else -> {
        val minutes = ageSeconds / 60L
        val seconds = ageSeconds % 60L
        if (seconds == 0L) "${minutes} min" else "${minutes} min ${seconds}s"
    }
}
