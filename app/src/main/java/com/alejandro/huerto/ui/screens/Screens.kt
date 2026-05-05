package com.alejandro.huerto.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alejandro.huerto.data.ActivityLogItem
import com.alejandro.huerto.data.ClimateHistoryUiPoint
import com.alejandro.huerto.data.HistorySeriesVisibility
import com.alejandro.huerto.data.HomeFreshnessUiState
import com.alejandro.huerto.data.HomeStatus
import com.alejandro.huerto.data.ManualControlUiState
import com.alejandro.huerto.data.SoilHistoryUiPoint
import com.alejandro.huerto.data.ValveState
import com.alejandro.huerto.ui.home.HomeViewModel
import com.alejandro.huerto.ui.theme.AquaDark
import com.alejandro.huerto.ui.theme.AquaLight
import com.alejandro.huerto.ui.theme.AlertWarm
import com.alejandro.huerto.ui.theme.FreshGreen

@Composable
fun DashboardScreen(
    onViewTemperatureHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val homeStatus by viewModel.homeStatus.collectAsState()
    val homeFreshness by viewModel.homeFreshness.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE7FAF7), Color(0xFFF6FBF8), Color.White),
                ),
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HeroStatusCard(homeStatus, homeFreshness)
        ClimateSection(homeStatus)
        SoilHumiditySection(homeStatus)
        PumpSection(homeStatus)
        Button(onClick = onViewTemperatureHistory, modifier = Modifier.fillMaxWidth()) {
            Text("Histórico 24h")
        }
    }
}

@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val homeStatus by viewModel.homeStatus.collectAsState()
    val homeFreshness by viewModel.homeFreshness.collectAsState()
    val manualControlState by viewModel.manualControlUiState.collectAsState()
    LaunchedEffect(homeStatus) {
        viewModel.clearManualRequestIfApplied(homeStatus)
    }
    LaunchedEffect(manualControlState.message, manualControlState.pendingValveIndex) {
        if (manualControlState.message != null && manualControlState.pendingValveIndex == null) {
            kotlinx.coroutines.delay(4_000)
            viewModel.dismissManualMessage()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Control manual", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ManualModeCard(homeStatus = homeStatus, manualState = manualControlState, freshness = homeFreshness)
        ValveSection(homeStatus.valves, onValveAction = { index ->
            viewModel.setValveManual(index, shouldOpen = true)
        })
    }
}

@Composable
fun ConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.configUiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.saveMessage, state.errorMessage) {
        if (state.saveMessage != null || state.errorMessage != null) {
            kotlinx.coroutines.delay(3_000)
            viewModel.dismissConfigMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FBFC))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Configuración", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Aquí se ajusta la calibración raw de cada línea y los porcentajes que gobiernan la lógica automática de riego.")
        MessageCard(
            message = "En modo MANUAL se puede superar el umbral máximo. Los porcentajes mínimo y máximo solo activan o paran la lógica automática.",
            isError = false,
        )

        state.errorMessage?.let { MessageCard(message = it, isError = true) }
        state.saveMessage?.let { MessageCard(message = it, isError = false) }

        ConfigSectionCard(title = "Lógica automática de riego") {
            ConfigNumberField(
                label = "% mínimo que activa riego automático",
                value = state.editor.automaticIrrigationThresholdMinPercent,
                onValueChange = { value -> viewModel.updateConfigEditor { it.copy(automaticIrrigationThresholdMinPercent = value) } },
            )
            ConfigNumberField(
                label = "% máximo que detiene riego automático",
                value = state.editor.automaticIrrigationThresholdMaxPercent,
                onValueChange = { value -> viewModel.updateConfigEditor { it.copy(automaticIrrigationThresholdMaxPercent = value) } },
            )
            ConfigNumberField(
                label = "Intervalo de lectura (ms)",
                value = state.editor.readingIntervalMs,
                onValueChange = { value -> viewModel.updateConfigEditor { it.copy(readingIntervalMs = value) } },
            )
        }

        SensorCalibrationCard(
            title = "Línea 1 / sensor suelo 1",
            rawValue = state.sensor1Live.raw,
            percentageValue = state.sensor1Live.percentage,
            minValue = state.editor.sensor1Min,
            maxValue = state.editor.sensor1Max,
            onMinChange = { value -> viewModel.updateConfigEditor { it.copy(sensor1Min = value) } },
            onMaxChange = { value -> viewModel.updateConfigEditor { it.copy(sensor1Max = value) } },
        )
        SensorCalibrationCard(
            title = "Línea 2 / sensor suelo 2",
            rawValue = state.sensor2Live.raw,
            percentageValue = state.sensor2Live.percentage,
            minValue = state.editor.sensor2Min,
            maxValue = state.editor.sensor2Max,
            onMinChange = { value -> viewModel.updateConfigEditor { it.copy(sensor2Min = value) } },
            onMaxChange = { value -> viewModel.updateConfigEditor { it.copy(sensor2Max = value) } },
        )
        SensorCalibrationCard(
            title = "Línea 3 / sensor suelo 3",
            rawValue = state.sensor3Live.raw,
            percentageValue = state.sensor3Live.percentage,
            minValue = state.editor.sensor3Min,
            maxValue = state.editor.sensor3Max,
            onMinChange = { value -> viewModel.updateConfigEditor { it.copy(sensor3Min = value) } },
            onMaxChange = { value -> viewModel.updateConfigEditor { it.copy(sensor3Max = value) } },
        )

        ConfigSectionCard(title = "Depósito") {
            ConfigNumberField(
                label = "Máximo tiempo de riego (ms)",
                value = state.editor.maxIrrigationMs,
                onValueChange = { value -> viewModel.updateConfigEditor { it.copy(maxIrrigationMs = value) } },
            )
            ConfigNumberField(
                label = "Delay reencendido llenado (ms)",
                value = state.editor.refillRestartDelayMs,
                onValueChange = { value -> viewModel.updateConfigEditor { it.copy(refillRestartDelayMs = value) } },
            )
        }

        Button(
            onClick = viewModel::saveConfig,
            enabled = !state.isLoading && !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) "Guardando..." else "Guardar en Firebase")
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun LogsScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val logs by viewModel.activityLogs.collectAsState()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FBFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Actividad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Vista técnica de eventos recientes del sistema.")
        }
        items(logs, key = { it.id }) { item ->
            ActivityLogCard(item)
        }
    }
}

@Composable
fun TemperatureHistoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val history by viewModel.climateHistory.collectAsState()
    val seriesVisibility by viewModel.historySeriesVisibility.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4FAFB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Últimas 24 horas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Evolución reciente de temperatura y humedad superior/inferior.")
        ClimateHistoryChart(
            history = history,
            visibility = seriesVisibility,
            onToggleSeries = { toggle -> viewModel.toggleHistorySeries(toggle) },
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(history, key = { it.timeLabel + it.superiorTemperature + it.inferiorTemperature }) { point ->
                ClimateHistoryRow(point)
            }
        }
        Button(onClick = onNavigateBack, contentPadding = PaddingValues(horizontal = 32.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroStatusCard(homeStatus: HomeStatus, freshness: HomeFreshnessUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F6D73), Color(0xFF14927E), Color(0xFF74B63E)),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Huerto Alejandro", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF7FFFE), fontWeight = FontWeight.Bold)
                Text("Estado general", color = Color(0xFFE6FBF8), fontSize = 12.sp)
                FreshnessBanner(freshness = freshness)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(label = "Modo", value = homeStatus.irrigationMode, accent = Color(0xFFF7FFFE))
                    StatusPill(label = "Depósito", value = if (homeStatus.lowLevel) "BAJO" else "OK", accent = if (homeStatus.lowLevel) Color(0xFFFFE0A8) else Color(0xFFF7FFFE))
                    StatusPill(label = "Impulsión", value = if (homeStatus.impulsePumpOn) "ON" else "OFF", accent = Color(0xFFF7FFFE))
                }
            }
        }
    }
}

@Composable
private fun ClimateSection(homeStatus: HomeStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Clima")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ClimateCard(
                modifier = Modifier.weight(1f),
                title = "Superior",
                temperature = homeStatus.climate.superiorTemperature?.let { "${it.toInt()} °C" } ?: "--",
                humidity = homeStatus.climate.superiorHumidity?.let { "${it.toInt()} %" } ?: "--",
            )
            ClimateCard(
                modifier = Modifier.weight(1f),
                title = "Inferior",
                temperature = homeStatus.climate.inferiorTemperature?.let { "${it.toInt()} °C" } ?: "--",
                humidity = homeStatus.climate.inferiorHumidity?.let { "${it.toInt()} %" } ?: "--",
            )
        }
    }
}

@Composable
private fun SoilHumiditySection(homeStatus: HomeStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Humedad suelo")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            homeStatus.soilSensors.forEachIndexed { index, sensor ->
                SoilHumidityCompactCard(
                    modifier = Modifier.weight(1f),
                    lineLabel = "L${index + 1}",
                    rawValue = sensor.raw?.toString() ?: "--",
                    percentageValue = sensor.percentage?.let { "$it %" } ?: "--",
                )
            }
        }
    }
}

@Composable
private fun ClimateCard(modifier: Modifier, title: String, temperature: String, humidity: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            MetricRow(Icons.Default.DeviceThermostat, "Temperatura", temperature)
            MetricRow(Icons.Default.Opacity, "Humedad", humidity)
        }
    }
}

@Composable
private fun ValveSection(valves: List<ValveState>, onValveAction: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Válvulas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        valves.forEachIndexed { index, valve ->
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusDot(active = valve.isOn)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(valve.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (valve.isOn) "Encendida" else "Apagada")
                    }
                    Button(onClick = { onValveAction(index + 1) }) {
                        Text(if (valve.isOn) "Reactivar" else "Activar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PumpSection(homeStatus: HomeStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Bombas")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricStatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Power,
                label = "Impulsión",
                value = if (homeStatus.impulsePumpOn) "ON" else "OFF",
                active = homeStatus.impulsePumpOn,
            )
            MetricStatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WaterDrop,
                label = "Llenado",
                value = if (homeStatus.fillPumpOn) "ON" else "OFF",
                active = homeStatus.fillPumpOn,
            )
        }
    }
}

@Composable
private fun MetricStatusCard(modifier: Modifier, icon: ImageVector, label: String, value: String, active: Boolean) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricRow(icon, label, value)
            StatusDot(active = active)
        }
    }
}

@Composable
private fun MetricRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(icon, contentDescription = label, tint = AquaDark, modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, accent: Color) {
    Surface(color = Color(0x1AFFFFFF), shape = RoundedCornerShape(999.dp)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, color = Color(0xFFD8F3F0), style = MaterialTheme.typography.labelSmall)
            Text(value, color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SoilHumidityCompactCard(
    modifier: Modifier,
    lineLabel: String,
    rawValue: String,
    percentageValue: String,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(lineLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = percentageValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AquaDark,
            )
            Text("raw", style = MaterialTheme.typography.labelSmall, color = Color(0xFF66777C))
            Text(rawValue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(if (active) FreshGreen else Color.LightGray),
    )
}

@Composable
private fun ClimateHistoryRow(point: ClimateHistoryUiPoint) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(point.timeLabel, fontWeight = FontWeight.Bold)
                androidx.compose.material3.Icon(Icons.Default.History, contentDescription = null, tint = AquaDark)
            }
            HorizontalDivider()
            Text("Sup: ${point.superiorTemperature} °C · ${point.superiorHumidity} %")
            Text("Inf: ${point.inferiorTemperature} °C · ${point.inferiorHumidity} %")
        }
    }
}

@Composable
private fun ClimateHistoryChart(
    history: List<ClimateHistoryUiPoint>,
    visibility: HistorySeriesVisibility,
    onToggleSeries: (((HistorySeriesVisibility) -> HistorySeriesVisibility)) -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gráfico 24h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (history.size < 2) {
                Text("Aún no hay suficientes muestras para dibujar la gráfica.")
            } else {
                ClimateLegend(visibility = visibility, onToggleSeries = onToggleSeries)
                DetailedClimateHistoryChart(history = history, visibility = visibility)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClimateLegend(
    visibility: HistorySeriesVisibility,
    onToggleSeries: (((HistorySeriesVisibility) -> HistorySeriesVisibility)) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendChip(
            label = "Temp superior",
            color = AquaDark,
            selected = visibility.superiorTemperature,
            onClick = { onToggleSeries { it.copy(superiorTemperature = !it.superiorTemperature) } },
        )
        LegendChip(
            label = "Temp inferior",
            color = FreshGreen,
            selected = visibility.inferiorTemperature,
            onClick = { onToggleSeries { it.copy(inferiorTemperature = !it.inferiorTemperature) } },
        )
        LegendChip(
            label = "Humedad superior",
            color = AlertWarm,
            selected = visibility.superiorHumidity,
            onClick = { onToggleSeries { it.copy(superiorHumidity = !it.superiorHumidity) } },
        )
        LegendChip(
            label = "Humedad inferior",
            color = AquaLight,
            selected = visibility.inferiorHumidity,
            onClick = { onToggleSeries { it.copy(inferiorHumidity = !it.inferiorHumidity) } },
        )
    }
}

@Composable
private fun LegendChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        },
    )
}

@Composable
private fun DetailedClimateHistoryChart(
    history: List<ClimateHistoryUiPoint>,
    visibility: HistorySeriesVisibility,
) {
    val visibleSeries = remember(history, visibility) {
        buildList {
            if (visibility.superiorTemperature) add(SeriesDefinition("Temp sup", AquaDark, history.map { it.superiorTemperature.toFloat() }))
            if (visibility.inferiorTemperature) add(SeriesDefinition("Temp inf", FreshGreen, history.map { it.inferiorTemperature.toFloat() }))
            if (visibility.superiorHumidity) add(SeriesDefinition("Hum sup", AlertWarm, history.map { it.superiorHumidity.toFloat() }))
            if (visibility.inferiorHumidity) add(SeriesDefinition("Hum inf", AquaLight, history.map { it.inferiorHumidity.toFloat() }))
        }
    }

    if (visibleSeries.isEmpty()) {
        Text("Selecciona al menos una serie para visualizar el histórico.")
        return
    }

    val allValues = visibleSeries.flatMap { it.values }
    val minValue = allValues.minOrNull() ?: 0f
    val maxValue = allValues.maxOrNull() ?: 100f
    val rangePadding = ((maxValue - minValue) * 0.1f).coerceAtLeast(2f)
    val chartMin = minValue - rangePadding
    val chartMax = maxValue + rangePadding
    val markerIndices = listOf(0, history.size / 3, (history.size * 2) / 3, history.lastIndex).distinct()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier.height(220.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            listOf(chartMax, (chartMax + chartMin) / 2f, chartMin).forEach { value ->
                Text(text = value.toInt().toString(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF6D7B80))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            ) {
                val horizontalGuides = 4
                val leftPadding = 12f
                val rightPadding = 12f
                val topPadding = 8f
                val bottomPadding = 12f
                val drawableWidth = size.width - leftPadding - rightPadding
                val drawableHeight = size.height - topPadding - bottomPadding
                val widthStep = drawableWidth / (history.size - 1).coerceAtLeast(1)
                val valueRange = (chartMax - chartMin).takeIf { it > 0f } ?: 1f

                repeat(horizontalGuides + 1) { index ->
                    val y = topPadding + (drawableHeight / horizontalGuides) * index
                    drawLine(
                        color = Color(0xFFDDE8EB),
                        start = Offset(leftPadding, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = 1.5f,
                    )
                }

                markerIndices.forEach { index ->
                    val x = leftPadding + widthStep * index
                    drawLine(
                        color = Color(0xFFE6EEF0),
                        start = Offset(x, topPadding),
                        end = Offset(x, size.height - bottomPadding),
                        strokeWidth = 1f,
                    )
                }

                fun yFor(value: Float): Float {
                    val normalized = (value - chartMin) / valueRange
                    return topPadding + drawableHeight - (normalized * drawableHeight)
                }

                visibleSeries.forEach { series ->
                    val path = Path()
                    series.values.forEachIndexed { index, value ->
                        val x = leftPadding + widthStep * index
                        val y = yFor(value)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path = path, color = series.color, style = Stroke(width = 4f, cap = StrokeCap.Round))

                    series.values.forEachIndexed { index, value ->
                        if (index in markerIndices) {
                            drawCircle(
                                color = series.color,
                                radius = 4.5f,
                                center = Offset(leftPadding + widthStep * index, yFor(value)),
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                markerIndices.forEach { index ->
                    Text(
                        text = history[index].timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6D7B80),
                        modifier = Modifier.widthIn(min = 40.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualModeCard(homeStatus: HomeStatus, manualState: ManualControlUiState, freshness: HomeFreshnessUiState) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (homeStatus.irrigationMode == "MANUAL") Color(0xFFFFF2E4) else Color(0xFFE8F6F1),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (homeStatus.irrigationMode == "MANUAL") "Modo manual activo" else "Modo automático activo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when {
                    manualState.message != null -> manualState.message
                    manualState.pendingValveIndex != null -> "Solicitud enviada para V${manualState.pendingValveIndex}. Esperando confirmación física en telemetría."
                    homeStatus.irrigationMode == "MANUAL" -> "Las órdenes manuales se reflejan cuando la válvula cambia a ON en el depósito."
                    else -> "Al activar una válvula desde aquí, la app fuerza MANUAL y envía la orden a Firebase."
                },
                color = if (manualState.isError) Color(0xFF8C1D18) else LocalContentColor.current,
            )
            Text(
                text = freshness.message,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    freshness.isOffline -> Color(0xFF8C1D18)
                    freshness.isStale -> AlertWarm
                    else -> Color(0xFF4D6B70)
                },
            )
        }
    }
}

@Composable
private fun FreshnessBanner(freshness: HomeFreshnessUiState) {
    val background = when {
        freshness.isOffline -> Color(0x26FFC9C6)
        freshness.isStale -> Color(0x26FFE0A8)
        else -> Color(0x1AF7FFFE)
    }
    val textColor = when {
        freshness.isOffline -> Color(0xFFFFE9E7)
        freshness.isStale -> Color(0xFFFFF3D8)
        else -> Color(0xFFEAFBFA)
    }

    Surface(color = background, shape = RoundedCornerShape(14.dp)) {
        Text(
            text = freshness.message,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

private data class SeriesDefinition(
    val label: String,
    val color: Color,
    val values: List<Float>,
)

@Composable
private fun ActivityLogCard(item: ActivityLogItem) {
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.type.ifBlank { "evento" }, fontWeight = FontWeight.Bold)
                Text(item.serverTimestamp?.let { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "sin hora")
            }
            Text(item.detail.ifBlank { "Sin detalle" })
            Text("origen: ${item.origin.ifBlank { "desconocido" }}")
            item.line?.let { Text("línea: $it") }
            item.state?.let { Text("estado: $it") }
        }
    }
}

@Composable
private fun ConfigSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SensorCalibrationCard(
    title: String,
    rawValue: Int?,
    percentageValue: Int?,
    minValue: String,
    maxValue: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
) {
    ConfigSectionCard(title = title) {
        LiveSensorInfoRow(rawValue = rawValue, percentageValue = percentageValue)
        Text(
            text = "Estos valores raw definen la calibración del sensor: mínimo = 0% y máximo = 100%.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5C7176),
        )
        ConfigNumberField(label = "Raw mínimo (0%)", value = minValue, onValueChange = onMinChange)
        ConfigNumberField(label = "Raw máximo (100%)", value = maxValue, onValueChange = onMaxChange)
    }
}

@Composable
private fun LiveSensorInfoRow(rawValue: Int?, percentageValue: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        LiveMetricCard(
            modifier = Modifier.weight(1f),
            label = "Raw actual",
            value = rawValue?.toString() ?: "--",
        )
        LiveMetricCard(
            modifier = Modifier.weight(1f),
            label = "% actual",
            value = percentageValue?.let { "$it %" } ?: "--",
        )
    }
}

@Composable
private fun LiveMetricCard(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F7F8)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF587177))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConfigNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { updated ->
            if (updated.all { it.isDigit() }) {
                onValueChange(updated)
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MessageCard(message: String, isError: Boolean) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) Color(0xFFFFE7E7) else Color(0xFFE8F7EE),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = if (isError) Color(0xFF8B1E1E) else Color(0xFF145C2B),
        )
    }
}

@Composable
fun HistoricoScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4FAFB))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Histórico", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Datos climáticos a largo plazo y evolución de humedad de suelo.")

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Clima — últimas 24h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Los datos agregados a largo plazo estarán disponibles cuando el script de agregación se ejecute periódicamente.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF587177))
                ClimateHistorySection24h(viewModel)
            }
        }

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Humedad de suelo — últimas 24h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SoilHistorySection24h(viewModel)
            }
        }
    }
}

@Composable
private fun ClimateHistorySection24h(viewModel: HomeViewModel) {
    val history by viewModel.climateHistory.collectAsState()
    val seriesVisibility by viewModel.historySeriesVisibility.collectAsState()

    if (history.isEmpty()) {
        Text("Sin datos de clima en las últimas 24h.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF587177))
        return
    }

    ClimateHistoryChart(
        history = history,
        visibility = seriesVisibility,
        onToggleSeries = { toggle -> viewModel.toggleHistorySeries(toggle) },
    )
}

@Composable
private fun SoilHistorySection24h(viewModel: HomeViewModel) {
    val soilHistory by viewModel.soilHistory.collectAsState()

    if (soilHistory.isEmpty()) {
        Text("Sin datos de humedad de suelo en las últimas 24h.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF587177))
        return
    }

    SoilHistoryChart(history = soilHistory)
}

@Composable
private fun SoilHistoryChart(history: List<SoilHistoryUiPoint>) {
    val maxRaw = history.maxOfOrNull { maxOf(it.sensor1, it.sensor2, it.sensor3) } ?: 30000
    val minRaw = history.minOfOrNull { minOf(it.sensor1, it.sensor2, it.sensor3) } ?: 8000
    val range = (maxRaw - minRaw).coerceAtLeast(1)

    val lineColors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800))
    val lineLabels = listOf("L1", "L2", "L3")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            val width = size.width
            val height = size.height
            val padding = 12f
            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            val points = history.mapIndexed { index, point ->
                val x = padding + (index.toFloat() / (history.size - 1).coerceAtLeast(1)) * chartWidth
                listOf(
                    Offset(x, padding + chartHeight - ((point.sensor1 - minRaw).toFloat() / range) * chartHeight),
                    Offset(x, padding + chartHeight - ((point.sensor2 - minRaw).toFloat() / range) * chartHeight),
                    Offset(x, padding + chartHeight - ((point.sensor3 - minRaw).toFloat() / range) * chartHeight),
                )
            }

            for (lineIndex in 0..2) {
                val path = Path()
                points.forEachIndexed { i, pts ->
                    if (i == 0) path.moveTo(pts[lineIndex].x, pts[lineIndex].y)
                    else path.lineTo(pts[lineIndex].x, pts[lineIndex].y)
                }
                drawPath(
                    path = path,
                    color = lineColors[lineIndex],
                    style = Stroke(width = 2f, cap = StrokeCap.Round),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(start = 8.dp)) {
            lineLabels.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(12.dp).background(lineColors[i], CircleShape))
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
