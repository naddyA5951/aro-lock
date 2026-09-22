package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room Entity representing a completed outdoor trek.
 * Stores route coordinates as a lightweight JSON/compact string so it can be re-rendered on the map anytime offline.
 */
@Entity(tableName = "treks")
data class TrekEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val difficulty: String, // "Easy", "Moderate", "Challenging", "Expert"
    val notes: String,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val durationSeconds: Long,
    val caloriesKcal: Int,
    val startTimestamp: Long,
    val endTimestamp: Long = System.currentTimeMillis(),
    val encodedTrailPoints: String = "", // Compact serialized coordinates
    val photoUri: String? = null
)
