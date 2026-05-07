package com.alejandro.huerto.ui.home

import com.alejandro.huerto.data.ConfigEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelLogicConfigTest {

    @Test
    fun `valid config returns parsed system config`() {
        val result = validEditorState().toSystemConfigOrError()

        assertTrue(result is ConfigValidationResult.Success)
        val config = (result as ConfigValidationResult.Success).config
        assertEquals(35, config.huerto.automaticIrrigationThresholdMinPercent)
        assertEquals(70, config.huerto.automaticIrrigationThresholdMaxPercent)
        assertEquals(300_000, config.huerto.intervaloLecturaClimaMs)
        assertEquals(30_000, config.huerto.intervaloLecturaSueloMs)
        assertEquals(900_000, config.deposito.maxIrrigationMs)
    }

    @Test
    fun `invalid threshold range returns expected error`() {
        val result = validEditorState(
            automaticIrrigationThresholdMinPercent = "80",
            automaticIrrigationThresholdMaxPercent = "70",
        ).toSystemConfigOrError()

        assertEquals(
            "El umbral mínimo debe ser menor que el máximo para que la lógica automática tenga histéresis.",
            (result as ConfigValidationResult.Error).message,
        )
    }

    @Test
    fun `invalid soil interval returns expected error`() {
        val result = validEditorState(intervaloLecturaSueloMs = "4000").toSystemConfigOrError()

        assertEquals(
            "El intervalo de suelo debe quedar entre 5 s y 5 min.",
            (result as ConfigValidationResult.Error).message,
        )
    }

    @Test
    fun `sensor calibration requires min greater than max`() {
        val error = validateSensorCalibration(sensorLabel = "sensor 1", minValue = 9000, maxValue = 9000)

        assertEquals(
            "La calibración del sensor 1 debe cumplir min > max para representar seco > húmedo.",
            error,
        )
    }

    @Test
    fun `sensor calibration requires enough margin`() {
        val error = validateSensorCalibration(sensorLabel = "sensor 2", minValue = 10000, maxValue = 9600)

        assertEquals(
            "La calibración del sensor 2 tiene un margen demasiado pequeño. Amplía la diferencia entre min y max.",
            error,
        )
    }

    private fun validEditorState(
        automaticIrrigationThresholdMinPercent: String = "35",
        automaticIrrigationThresholdMaxPercent: String = "70",
        readingIntervalMs: String = "10000",
        intervaloLecturaClimaMs: String = "300000",
        intervaloLecturaSueloMs: String = "30000",
        sensor1Min: String = "26000",
        sensor1Max: String = "8000",
        sensor2Min: String = "26500",
        sensor2Max: String = "8500",
        sensor3Min: String = "27000",
        sensor3Max: String = "9000",
        maxIrrigationMs: String = "900000",
        refillRestartDelayMs: String = "30000",
    ) = ConfigEditorState(
        automaticIrrigationThresholdMinPercent = automaticIrrigationThresholdMinPercent,
        automaticIrrigationThresholdMaxPercent = automaticIrrigationThresholdMaxPercent,
        readingIntervalMs = readingIntervalMs,
        intervaloLecturaClimaMs = intervaloLecturaClimaMs,
        intervaloLecturaSueloMs = intervaloLecturaSueloMs,
        sensor1Min = sensor1Min,
        sensor1Max = sensor1Max,
        sensor2Min = sensor2Min,
        sensor2Max = sensor2Max,
        sensor3Min = sensor3Min,
        sensor3Max = sensor3Max,
        maxIrrigationMs = maxIrrigationMs,
        refillRestartDelayMs = refillRestartDelayMs,
    )
}
