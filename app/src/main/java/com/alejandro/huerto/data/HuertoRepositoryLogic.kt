package com.alejandro.huerto.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class ClimateAggregateBucketRaw(
    val bucketStartTs: Long?,
    val superiorTemperatureAvg: Double?,
    val superiorHumidityAvg: Double?,
    val inferiorTemperatureAvg: Double?,
    val inferiorHumidityAvg: Double?,
)

internal fun normalizeClimateAggregateBuckets(
    rawBuckets: List<ClimateAggregateBucketRaw>,
    timeFormatter: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()),
): List<ClimateHistoryUiPoint> {
    return rawBuckets.mapNotNull { bucket ->
        val ts = bucket.bucketStartTs ?: return@mapNotNull null
        val superiorTemp = bucket.superiorTemperatureAvg?.toRoundedIntOrNull() ?: return@mapNotNull null
        val superiorHum = bucket.superiorHumidityAvg?.toRoundedIntOrNull() ?: return@mapNotNull null
        val inferiorTemp = bucket.inferiorTemperatureAvg?.toRoundedIntOrNull() ?: return@mapNotNull null
        val inferiorHum = bucket.inferiorHumidityAvg?.toRoundedIntOrNull() ?: return@mapNotNull null
        ClimateHistoryUiPoint(
            timestamp = ts,
            timeLabel = timeFormatter.format(Date(ts)),
            superiorTemperature = superiorTemp,
            superiorHumidity = superiorHum,
            inferiorTemperature = inferiorTemp,
            inferiorHumidity = inferiorHum,
        )
    }.sortedBy { it.timestamp }
}
