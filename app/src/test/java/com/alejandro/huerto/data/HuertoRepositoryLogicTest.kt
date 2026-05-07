package com.alejandro.huerto.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class HuertoRepositoryLogicTest {

    @Test
    fun `normalize aggregate buckets sorts by timestamp and rounds values`() {
        val formatter = SimpleDateFormat("HH:mm", Locale.US)
        val result = normalizeClimateAggregateBuckets(
            listOf(
                ClimateAggregateBucketRaw(
                    bucketStartTs = 2_000L,
                    superiorTemperatureAvg = 21.6,
                    superiorHumidityAvg = 61.4,
                    inferiorTemperatureAvg = 19.4,
                    inferiorHumidityAvg = 70.6,
                ),
                ClimateAggregateBucketRaw(
                    bucketStartTs = 1_000L,
                    superiorTemperatureAvg = 20.2,
                    superiorHumidityAvg = 60.2,
                    inferiorTemperatureAvg = 18.8,
                    inferiorHumidityAvg = 69.8,
                ),
            ),
            timeFormatter = formatter,
        )

        assertEquals(listOf(1_000L, 2_000L), result.map { it.timestamp })
        assertEquals(20, result.first().superiorTemperature)
        assertEquals(70, result.first().inferiorHumidity)
    }

    @Test
    fun `normalize aggregate buckets skips incomplete bucket`() {
        val result = normalizeClimateAggregateBuckets(
            listOf(
                ClimateAggregateBucketRaw(
                    bucketStartTs = 1_000L,
                    superiorTemperatureAvg = 20.0,
                    superiorHumidityAvg = 60.0,
                    inferiorTemperatureAvg = 18.0,
                    inferiorHumidityAvg = 70.0,
                ),
                ClimateAggregateBucketRaw(
                    bucketStartTs = 2_000L,
                    superiorTemperatureAvg = 21.0,
                    superiorHumidityAvg = null,
                    inferiorTemperatureAvg = 19.0,
                    inferiorHumidityAvg = 71.0,
                ),
            ),
        )

        assertEquals(1, result.size)
        assertEquals(1_000L, result.single().timestamp)
    }
}
