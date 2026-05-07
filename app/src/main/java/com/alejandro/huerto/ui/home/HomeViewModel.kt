package com.alejandro.huerto.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejandro.huerto.data.ActivityLogItem
import com.alejandro.huerto.data.ClimateHistoryUiPoint
import com.alejandro.huerto.data.ClimateSelectedDay
import com.alejandro.huerto.data.ConfigEditorState
import com.alejandro.huerto.data.ConfigUiState
import com.alejandro.huerto.data.DepositoConfig
import com.alejandro.huerto.data.HistorySeriesVisibility
import com.alejandro.huerto.data.HomeFreshnessUiState
import com.alejandro.huerto.data.HomeStatus
import com.alejandro.huerto.data.HuertoRepository
import com.alejandro.huerto.data.ManualControlUiState
import com.alejandro.huerto.data.SoilHistoryUiPoint
import com.alejandro.huerto.data.SoilSensorLiveReading
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
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    private val selectedClimateDayState = MutableStateFlow(currentClimateSelectedDay())
    val selectedClimateDay: StateFlow<ClimateSelectedDay> = selectedClimateDayState

    private val comparisonEnabledState = MutableStateFlow(false)
    val comparisonEnabled: StateFlow<Boolean> = comparisonEnabledState

    private val comparisonClimateDayState = MutableStateFlow(currentClimateSelectedDay(dayOffset = -1))
    val comparisonClimateDay: StateFlow<ClimateSelectedDay> = comparisonClimateDayState

    val selectedDayClimateHistory: StateFlow<List<ClimateHistoryUiPoint>> = repository
        .observeClimateAggregateDay(selectedClimateDayState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val comparisonDayClimateHistory: StateFlow<List<ClimateHistoryUiPoint>> = repository
        .observeClimateAggregateDay(comparisonClimateDayState)
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
                    message = unresolvedManualRequestMessage(homeStatus.value.lowLevel),
                    isError = true,
                )
            }
        }
    }

    fun clearManualRequestIfApplied(homeStatus: HomeStatus) {
        resolveManualRequestConfirmation(manualControlState.value, homeStatus)?.let {
            manualControlState.value = it
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

    fun setSelectedClimateDay(year: Int, month: Int, day: Int) {
        selectedClimateDayState.value = ClimateSelectedDay(year = year, month = month, day = day)
    }

    fun setSelectedClimateDayToToday() {
        selectedClimateDayState.value = currentClimateSelectedDay()
    }

    fun setComparisonEnabled(enabled: Boolean) {
        comparisonEnabledState.value = enabled
    }

    fun setComparisonClimateDay(year: Int, month: Int, day: Int) {
        comparisonClimateDayState.value = ClimateSelectedDay(year = year, month = month, day = day)
    }

    fun setComparisonClimateDayToYesterday() {
        comparisonClimateDayState.value = currentClimateSelectedDay(dayOffset = -1)
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
            is ConfigValidationResult.Error -> {
                saveState.value = saveState.value.copy(errorMessage = validation.message)
                return
            }
            is ConfigValidationResult.Success -> validation.config
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

    private fun tickerFlow() = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }
}
