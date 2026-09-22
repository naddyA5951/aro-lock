package com.example.service

import com.example.model.TrailPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-precision metrics calculation engine for trekking and hiking.
 * Includes GPS noise filtering, elevation fluctuation thresholds, and calorie estimation.
 */
object TrekMetricsEngine {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Threshold to ignore micro-altitude GPS noise.
     * Raw GPS altitude is notoriously noisy (often ±5m accuracy variance even when standing still).
     * We only count climbing/descending if the altitude delta between consecutive stable points exceeds this threshold.
     */
    private const val ELEVATION_NOISE_THRESHOLD_METERS = 2.5

    /**
     * Maximum reasonable speed for a human trekker (approx 25 km/h or ~7.0 m/s).
     * Speeds exceeding this typically indicate an errant GPS jump (e.g. satellite multipath in valleys).
     */
    private const val MAX_PLAUSIBLE_TREKKING_SPEED_MS = 7.0

    /**
     * Calculates the great-circle distance between two coordinates using the Haversine formula.
     * Returns distance in meters.
     */
    fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    data class TrekComputedStats(
        val totalDistanceMeters: Double = 0.0,
        val elevationGainMeters: Double = 0.0,
        val elevationLossMeters: Double = 0.0,
        val currentElevationMeters: Double = 0.0,
        val currentSpeedKmh: Double = 0.0,
        val averageSpeedKmh: Double = 0.0,
        val estimatedCaloriesKcal: Int = 0
    )

    /**
     * Computes all cumulative trail metrics across the provided sequence of GPS points.
     *
     * @param points Ordered list of GPS points recorded during the trek.
     * @param durationSeconds Active trekking elapsed duration.
     * @param userWeightKg Hiker body weight (defaults to 70 kg standard).
     */
    fun computeStats(
        points: List<TrailPoint>,
        durationSeconds: Long,
        userWeightKg: Double = 70.0
    ): TrekComputedStats {
        if (points.isEmpty()) {
            return TrekComputedStats()
        }

        var totalDist = 0.0
        var totalGain = 0.0
        var totalLoss = 0.0
        var referenceAlt = points.first().altitude

        for (i in 1 until points.size) {
            val pPrev = points[i - 1]
            val pCurr = points[i]

            // 1. Distance with GPS jump filtering
            val legDist = calculateHaversineDistance(
                pPrev.latitude, pPrev.longitude,
                pCurr.latitude, pCurr.longitude
            )

            val timeDeltaSeconds = max(1.0, (pCurr.timestamp - pPrev.timestamp) / 1000.0)
            val impliedSpeed = legDist / timeDeltaSeconds

            // Filter out erratic satellite jumps (> 25 km/h or poor accuracy > 40m)
            val isAccurate = (pCurr.accuracy == 0f || pCurr.accuracy <= 40f)
            if (isAccurate && impliedSpeed <= MAX_PLAUSIBLE_TREKKING_SPEED_MS && legDist >= 1.5) {
                totalDist += legDist
            }

            // 2. Elevation with noise smoothing
            val altDelta = pCurr.altitude - referenceAlt
            if (altDelta >= ELEVATION_NOISE_THRESHOLD_METERS) {
                totalGain += altDelta
                referenceAlt = pCurr.altitude
            } else if (altDelta <= -ELEVATION_NOISE_THRESHOLD_METERS) {
                totalLoss += -altDelta
                referenceAlt = pCurr.altitude
            }
        }

        val lastPoint = points.last()
        val currentElevation = lastPoint.altitude
        val currentSpeedKmh = (lastPoint.speed * 3.6).coerceAtLeast(0.0)

        val durationHours = max(0.0001, durationSeconds / 3600.0)
        val distanceKm = totalDist / 1000.0
        val averageSpeedKmh = distanceKm / durationHours

        // 3. Calorie estimation using Metabolic Equivalent of Task (MET)
        // Standard hiking is approx 6.0 METs; steep climbing increases to 7.5 - 9.0 METs.
        val inclineFactor = if (totalDist > 0) (totalGain / totalDist) * 10.0 else 0.0
        val baseMet = 6.0 + inclineFactor.coerceIn(0.0, 3.5)
        val calories = (baseMet * userWeightKg * durationHours).toInt()

        return TrekComputedStats(
            totalDistanceMeters = totalDist,
            elevationGainMeters = totalGain,
            elevationLossMeters = totalLoss,
            currentElevationMeters = currentElevation,
            currentSpeedKmh = currentSpeedKmh,
            averageSpeedKmh = averageSpeedKmh.coerceAtMost(25.0),
            estimatedCaloriesKcal = calories
        )
    }

    /**
     * Formats seconds into standard outdoor display 00:00:00
     */
    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
