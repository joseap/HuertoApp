package com.alejandro.huerto.ui.home

import com.alejandro.huerto.data.HomeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelLogicFreshnessTest {

    @Test
    fun `fresh telemetry is not stale`() {
        val now = 1_000_000L
        val status = HomeStatus(lastUpdatedAtMs = now - 10_000L)

        val freshness = status.toFreshness(now)

        assertFalse(freshness.isStale)
        assertFalse(freshness.isOffline)
        assertEquals("Actualizado hace 10s.", freshness.message)
    }

    @Test
    fun `stale telemetry keeps last online state`() {
        val now = 1_000_000L
        val status = HomeStatus(lastUpdatedAtMs = now - 45_000L)

        val freshness = status.toFreshness(now)

        assertTrue(freshness.isStale)
        assertFalse(freshness.isOffline)
        assertEquals("Sin datos recientes. Última actualización hace 45s.", freshness.message)
    }

    @Test
    fun `offline telemetry is marked as last known state`() {
        val now = 1_000_000L
        val status = HomeStatus(lastUpdatedAtMs = now - 120_000L)

        val freshness = status.toFreshness(now)

        assertTrue(freshness.isStale)
        assertTrue(freshness.isOffline)
        assertEquals("Mostrando último estado conocido. Sin actualización desde hace 2 min.", freshness.message)
    }

    @Test
    fun `missing telemetry reports waiting state`() {
        val freshness = HomeStatus(lastUpdatedAtMs = 0L).toFreshness(nowMs = 1_000_000L)

        assertTrue(freshness.isStale)
        assertFalse(freshness.isOffline)
        assertEquals("Esperando telemetría inicial del sistema.", freshness.message)
    }

    @Test
    fun `format elapsed handles mixed minutes and seconds`() {
        assertEquals("2 min 5s", formatElapsed(125L))
    }
}
