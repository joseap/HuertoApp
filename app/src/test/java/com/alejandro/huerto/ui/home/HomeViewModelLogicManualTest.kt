package com.alejandro.huerto.ui.home

import com.alejandro.huerto.data.HomeStatus
import com.alejandro.huerto.data.ManualControlUiState
import com.alejandro.huerto.data.ValveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelLogicManualTest {

    @Test
    fun `manual request confirmation returns success state when valve is on in manual mode`() {
        val currentState = ManualControlUiState(
            pendingValveIndex = 2,
            pendingOpenRequest = true,
            requestToken = 7L,
            message = "Orden enviada",
            isError = false,
        )
        val homeStatus = HomeStatus(
            irrigationMode = "MANUAL",
            valves = listOf(
                ValveState("V1", isOn = false),
                ValveState("V2", isOn = true),
                ValveState("V3", isOn = false),
            ),
        )

        val result = resolveManualRequestConfirmation(currentState, homeStatus)

        assertEquals("V2 confirmada en telemetría. Control manual activo.", result?.message)
        assertFalse(result!!.isError)
    }

    @Test
    fun `manual request confirmation returns null outside manual mode`() {
        val currentState = ManualControlUiState(pendingValveIndex = 1, pendingOpenRequest = true)
        val homeStatus = HomeStatus(irrigationMode = "AUTO")

        val result = resolveManualRequestConfirmation(currentState, homeStatus)

        assertNull(result)
    }

    @Test
    fun `manual unresolved message reports low level block`() {
        val message = unresolvedManualRequestMessage(lowLevel = true)

        assertTrue(message.contains("nivel bajo"))
    }

    @Test
    fun `manual unresolved message reports connectivity hint when no low level`() {
        val message = unresolvedManualRequestMessage(lowLevel = false)

        assertTrue(message.contains("telemetría"))
        assertTrue(message.contains("conectividad"))
    }
}
