package com.alejandro.huerto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(onViewTemperatureHistory: () -> Unit, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dashboard sensores\n(Aquí irán las cards de temperatura superior/inferior, nivel del depósito y humedad de suelo)",
            textAlign = TextAlign.Center
        )
        Button(onClick = onViewTemperatureHistory) {
            Text("Consultar histórico de temperaturas")
        }
    }
}

@Composable
fun ControlScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Control manual (modo global, válvulas, estados)",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConfigScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Configuración (umbrales, intervalos, calibraciones)",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LogsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Historial de eventos",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TemperatureHistoryScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Gráfica histórica de temperaturas superior/inferior",
            textAlign = TextAlign.Center
        )
        Button(onClick = onNavigateBack, contentPadding = PaddingValues(horizontal = 32.dp)) {
            Text("Volver")
        }
    }
}
