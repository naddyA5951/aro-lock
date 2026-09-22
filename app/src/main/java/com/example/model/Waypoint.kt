package com.example.model

/**
 * Represents a custom marked point of interest recorded along a trek.
 */
data class Waypoint(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val category: WaypointCategory,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class WaypointCategory(val label: String, val emoji: String) {
    VIEWPOINT("Scenic Viewpoint", "🏔️"),
    WATER_SOURCE("Water Source", "💧"),
    CAMPSITE("Campsite", "⛺"),
    REST_STOP("Rest Shelter", "🛖"),
    HAZARD("Trail Hazard", "⚠️"),
    LANDMARK("Landmark", "📍")
}
