package com.example.model

/**
 * Represents a single GPS point recorded during a trek.
 */
data class TrailPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
