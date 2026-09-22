package com.example.data

import com.example.model.TrailPoint
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer isolating UI and ViewModels from raw Room database access.
 */
class TrekRepository(private val trekDao: TrekDao) {

    val allTreks: Flow<List<TrekEntity>> = trekDao.getAllTreks()
    val trekCount: Flow<Int> = trekDao.getTrekCount()
    val totalDistanceMeters: Flow<Double> = trekDao.getTotalDistanceMeters()
    val totalElevationGainMeters: Flow<Double> = trekDao.getTotalElevationGainMeters()

    suspend fun saveTrek(trek: TrekEntity): Long {
        return trekDao.insertTrek(trek)
    }

    suspend fun deleteTrek(trek: TrekEntity) {
        trekDao.deleteTrek(trek)
    }

    suspend fun deleteTrekById(id: Long) {
        trekDao.deleteTrekById(id)
    }

    companion object {
        /**
         * Compact serializer for trail points: lat,lon,alt;lat,lon,alt
         */
        fun encodePoints(points: List<TrailPoint>): String {
            return points.joinToString(";") { "${it.latitude},${it.longitude},${it.altitude}" }
        }

        /**
         * Deserializer for encoded points back to TrailPoint list.
         */
        fun decodePoints(encoded: String): List<TrailPoint> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { part ->
                val tokens = part.split(",")
                if (tokens.size >= 2) {
                    val lat = tokens[0].toDoubleOrNull() ?: return@mapNotNull null
                    val lon = tokens[1].toDoubleOrNull() ?: return@mapNotNull null
                    val alt = if (tokens.size >= 3) tokens[2].toDoubleOrNull() ?: 0.0 else 0.0
                    TrailPoint(latitude = lat, longitude = lon, altitude = alt)
                } else null
            }
        }
    }
}
