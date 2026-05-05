package com.alejandro.huerto.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejandro.huerto.data.ActivityLogItem
import com.alejandro.huerto.data.ClimateHistoryUiPoint
import com.alejandro.huerto.data.ConfigEditorState
import com.alejandro.huerto.data.ConfigUiState
import com.alejandro.huerto.data.DepositoConfig
import com.alejandro.huerto.data.HistorySeriesVisibility
import com.alejandro.huerto.data.HomeFreshnessUiState
import com.alejandro.huerto.data.HomeStatus
import com.alejandro.huerto.data.HuertoConfig
import com.alejandro.huerto.data.HuertoRepository
import com.alejandro.huerto.data.ManualControlUiState
import com.alejandro.huerto.data.SoilHistoryUiPoint
import com.alejandro.huerto.data.SoilSensorLiveReading
import com.alejandro.huerto.data.SensorCalibration
import com.alejandro.huerto.data.SystemConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HuertoRepository,
) : ViewModel() {

    val homeStatus: StateFlow<HomeStatus> = repository.observeHomeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStatus())

    val homeFreshness: StateFlow<HomeFreshnessUiState> = combine(
        homeStatus,
        tickerFlow(),
    ) { status, now ->
        status.toFreshness(now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeFreshnessUiState())

    val activityLogs: StateFlow<List<ActivityLogItem>> = repository.observeActivityLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val climateHistory: StateFlow<List<ClimateHistoryUiPoint>> = repository.observeClimateHistoryLast24Hours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val soilHistory: StateFlow<List<SoilHistoryUiPoint>> = repository.observeSoilHistoryLast24Hours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val historySeriesVisibilityState = MutableStateFlow(HistorySeriesVisibility())
    val historySeriesVisibility: StateFlow<HistorySeriesVisibility> = historySeriesVisibilityState

    private val manualControlState = MutableStateFlow(ManualControlUiState())
    val manualControlUiState: StateFlow<ManualControlUiState> = manualControlState

    private var nextManualRequestToken: Long = 1L

    private val saveState = MutableStateFlow(ConfigSaveState())

    val configUiState: StateFlow<ConfigUiState> = combine(
        repository.observeSystemConfig(),
        repository.observeSoilSensorReadings(),
        saveState,
    ) { config, liveReadings, save ->
        ConfigUiState(
            isLoading = false,
            isSaving = save.isSaving,
            config = config,
            sensor1Live = liveReadings.getOrElse(0) { SoilSensorLiveReading() },
            sensor2Live = liveReadings.getOrElse(1) { SoilSensorLiveReading() },
            sensor3Live = liveReadings.getOrElse(2) { SoilSensorLiveReading() },
            editor = save.editor ?: config.toEditorState(),
            saveMessage = save.saveMessage,
            errorMessage = save.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfigUiState())

    fun setValveManual(valveIndex: Int, shouldOpen: Boolean) {
        val requestToken = nextManualRequestToken++
        manualControlState.value = ManualControlUiState(
            pendingValveIndex = valveIndex,
            pendingOpenRequest = shouldOpen,
            requestToken = requestToken,
            message = "Orden enviada para V$valveIndex. Esperando confirmación física.",
            isError = false,
        )
        viewModelScope.launch {
            runCatching {
                repository.setValveManual(valveIndex, shouldOpen)
            }.onFailure {
                manualControlState.value = ManualControlUiState(
                    message = it.message ?: "No se pudo enviar la orden manual.",
                    isError = true,
                )
                saveState.value = saveState.value.copy(
                    saveMessage = null,
                    errorMessage = it.message ?: "No se pudo enviar la orden manual.",
                )
            }
        }

        viewModelScope.launch {
            delay(8_000)
            val currentState = manualControlState.value
            if (currentState.requestToken == requestToken && currentState.pendingValveIndex == valveIndex) {
                manualControlState.value = ManualControlUiState(
                    message = if (homeStatus.value.lowLevel) {
                        "La orden manual no se confirmó. El depósito está en nivel bajo y el riego queda bloqueado."
                    } else {
                        "La orden manual no se confirmó todavía en telemetría. Revisa conectividad o estado del depósito."
                    },
                    isError = true,
                )
            }
        }
    }

    fun clearManualRequestIfApplied(homeStatus: HomeStatus) {
        val currentState = manualControlState.value
        val pending = currentState.pendingValveIndex ?: return
        if (homeStatus.irrigationMode != "MANUAL") return
        val requestedValve = homeStatus.valves.getOrNull(pending - 1) ?: return
        if (currentState.pendingOpenRequest && requestedValve.isOn) {
            manualControlState.value = ManualControlUiState(
                message = "V$pending confirmada en telemetría. Control manual activo.",
                isError = false,
            )
        }
    }

    fun dismissManualMessage() {
        if (manualControlState.value.pendingValveIndex == null) {
            manualControlState.value = ManualControlUiState()
        }
    }

    fun toggleHistorySeries(toggle: (HistorySeriesVisibility) -> HistorySeriesVisibility) {
        historySeriesVisibilityState.value = toggle(historySeriesVisibilityState.value)
    }

    fun updateConfigEditor(transform: (ConfigEditorState) -> ConfigEditorState) {
        val currentEditor = saveState.value.editor ?: configUiState.value.editor
        saveState.value = saveState.value.copy(
            editor = transform(currentEditor),
            saveMessage = null,
            errorMessage = null,
        )
    }

    fun saveConfig() {
        val editor = saveState.value.editor ?: configUiState.value.editor
        val validation = editor.toSystemConfigOrError()
        val parsed = when (validation) {
            is ValidationResult.Error -> {
                saveState.value = saveState.value.copy(errorMessage = validation.message)
                return
            }
            is ValidationResult.Success -> validation.config
        }

        viewModelScope.launch {
            saveState.value = saveState.value.copy(isSaving = true, saveMessage = null, errorMessage = null)
            runCatching {
                repository.saveSystemConfig(parsed)
            }.onSuccess {
                saveState.value = saveState.value.copy(
                    isSaving = false,
                    editor = parsed.toEditorState(),
                    saveMessage = "Configuración guardada en Firebase.",
                    errorMessage = null,
                )
            }.onFailure {
                saveState.value = saveState.value.copy(
                    isSaving = false,
                    saveMessage = null,
                    errorMessage = it.message ?: "No se pudo guardar la configuración.",
                )
            }
        }
    }

    fun dismissConfigMessage() {
        saveState.value = saveState.value.copy(saveMessage = null, errorMessage = null)
    }

    private data class ConfigSaveState(
        val isSaving: Boolean = false,
        val editor: ConfigEditorState? = null,
        val saveMessage: String? = null,
        val errorMessage: String? = null,
    )

    private fun SystemConfig.toEditorState(): ConfigEditorState = ConfigEditorState(
        automaticIrrigationThresholdMinPercent = huerto.automaticIrrigationThresholdMinPercent.toString(),
        automaticIrrigationThresholdMaxPercent = huerto.automaticIrrigationThresholdMaxPercent.toString(),
        readingIntervalMs = huerto.readingIntervalMs.toString(),
        intervaloLecturaClimaMs = huerto.intervaloLecturaClimaMs.toString(),
        intervaloLecturaSueloMs = huerto.intervaloLecturaSueloMs.toString(),
        sensor1Min = huerto.sensor1.min.toString(),
        sensor1Max = huerto.sensor1.max.toString(),
        sensor2Min = huerto.sensor2.min.toString(),
        sensor2Max = huerto.sensor2.max.toString(),
        sensor3Min = huerto.sensor3.min.toString(),
        sensor3Max = huerto.sensor3.max.toString(),
        maxIrrigationMs = deposito.maxIrrigationMs.toString(),
        refillRestartDelayMs = deposito.refillRestartDelayMs.toString(),
    )

    private fun ConfigEditorState.toSystemConfigOrError(): ValidationResult {
        val irrigationMinValue = automaticIrrigationThresholdMinPercent.toIntOrNull()
            ?: return ValidationResult.Error("El umbral mínimo debe ser un número entero.")
        val irrigationMaxValue = automaticIrrigationThresholdMaxPercent.toIntOrNull()
            ?: return ValidationResult.Error("El umbral máximo debe ser un número entero.")
        val readingIntervalValue = readingIntervalMs.toIntOrNull()
            ?: return ValidationResult.Error("El intervalo de lectura debe ser un número entero en milisegundos.")
        val climaIntervalValue = intervaloLecturaClimaMs.toIntOrNull()
            ?: return ValidationResult.Error("El intervalo de clima debe ser un número entero en milisegundos.")
        val sueloIntervalValue = intervaloLecturaSueloMs.toIntOrNull()
            ?: return ValidationResult.Error("El intervalo de suelo debe ser un número entero en milisegundos.")
        val sensor1MinValue = sensor1Min.toIntOrNull()
            ?: return ValidationResult.Error("La calibración mínima del sensor 1 debe ser un entero.")
        val sensor1MaxValue = sensor1Max.toIntOrNull()
            ?: return ValidationResult.Error("La calibración máxima del sensor 1 debe ser un entero.")
        val sensor2MinValue = sensor2Min.toIntOrNull()
            ?: return ValidationResult.Error("La calibración mínima del sensor 2 debe ser un entero.")
        val sensor2MaxValue = sensor2Max.toIntOrNull()
            ?: return ValidationResult.Error("La calibración máxima del sensor 2 debe ser un entero.")
        val sensor3MinValue = sensor3Min.toIntOrNull()
            ?: return ValidationResult.Error("La calibración mínima del sensor 3 debe ser un entero.")
        val sensor3MaxValue = sensor3Max.toIntOrNull()
            ?: return ValidationResult.Error("La calibración máxima del sensor 3 debe ser un entero.")
        val maxIrrigationValue = maxIrrigationMs.toIntOrNull()
            ?: return ValidationResult.Error("El máximo de riego debe ser un número entero en milisegundos.")
        val refillDelayValue = refillRestartDelayMs.toIntOrNull()
            ?: return ValidationResult.Error("El retardo de reencendido debe ser un número entero en milisegundos.")

        if (irrigationMinValue !in 0..100) {
            return ValidationResult.Error("El umbral mínimo debe estar entre 0% y 100%.")
        }
        if (irrigationMaxValue !in 0..100) {
            return ValidationResult.Error("El umbral máximo debe estar entre 0% y 100%.")
        }
        if (irrigationMinValue >= irrigationMaxValue) {
            return ValidationResult.Error("El umbral mínimo debe ser menor que el máximo para que la lógica automática tenga histéresis.")
        }
        if (readingIntervalValue !in 1_000..600_000) {
            return ValidationResult.Error("El intervalo de lectura debe quedar entre 1 s y 10 min.")
        }
        if (climaIntervalValue !in 30_000..1_800_000) {
            return ValidationResult.Error("El intervalo de clima debe quedar entre 30 s y 30 min.")
        }
        if (sueloIntervalValue !in 5_000..300_000) {
            return ValidationResult.Error("El intervalo de suelo debe quedar entre 5 s y 5 min.")
        }
        if (maxIrrigationValue !in 60_000..14_400_000) {
            return ValidationResult.Error("El máximo de riego debe quedar entre 1 min y 4 h.")
        }
        if (refillDelayValue !in 5_000..3_600_000) {
            return ValidationResult.Error("El retardo de reencendido debe quedar entre 5 s y 1 h.")
        }

        val sensorValidationError = validateSensorCalibration("sensor 1", sensor1MinValue, sensor1MaxValue)
            ?: validateSensorCalibration("sensor 2", sensor2MinValue, sensor2MaxValue)
            ?: validateSensorCalibration("sensor 3", sensor3MinValue, sensor3MaxValue)
        if (sensorValidationError != null) {
            return ValidationResult.Error(sensorValidationError)
        }

        return ValidationResult.Success(SystemConfig(
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
        ))
    }

    private fun validateSensorCalibration(sensorLabel: String, minValue: Int, maxValue: Int): String? {
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

    private sealed interface ValidationResult {
        data class Success(val config: SystemConfig) : ValidationResult
        data class Error(val message: String) : ValidationResult
    }

    private fun tickerFlow() = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    private fun HomeStatus.toFreshness(nowMs: Long): HomeFreshnessUiState {
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

    private fun formatElapsed(ageSeconds: Long): String = when {
        ageSeconds < 60L -> "${ageSeconds}s"
        else -> {
            val minutes = ageSeconds / 60L
            val seconds = ageSeconds % 60L
            if (seconds == 0L) "${minutes} min" else "${minutes} min ${seconds}s"
        }
    }
}
