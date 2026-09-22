package com.example.service

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.TrekEntity
import com.example.data.TrekRepository
import com.example.model.TrailPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates universal GPX (GPS Exchange Format) XML 1.1 files compatible with:
 * Strava, Garmin Connect, Google Earth, Gaia GPS, Komoot, and AllTrails.
 */
object GpxExportService {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

    /**
     * Converts a TrekEntity and its route points into valid GPX 1.1 XML string.
     */
    fun generateGpxXml(trek: TrekEntity, points: List<TrailPoint>): String {
        val createdIso = isoDateFormat.format(Date(trek.startTimestamp))
        val sb = StringBuilder()

        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="arolock outdoor app" """)
        sb.append("""xmlns="http://www.topografix.com/GPX/1/1" """)
        sb.append("""xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" """)
        sb.append("""xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">""").append("\n")

        // Metadata
        sb.append("  <metadata>\n")
        sb.append("    <name>${escapeXml(trek.title)}</name>\n")
        sb.append("    <desc>${escapeXml(trek.notes)}</desc>\n")
        sb.append("    <time>$createdIso</time>\n")
        sb.append("  </metadata>\n")

        // Track
        sb.append("  <trk>\n")
        sb.append("    <name>${escapeXml(trek.title)}</name>\n")
        sb.append("    <type>Hiking</type>\n")
        sb.append("    <trkseg>\n")

        for (pt in points) {
            val pointIso = isoDateFormat.format(Date(pt.timestamp))
            sb.append(
                String.format(
                    Locale.US,
                    "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n        <ele>%.1f</ele>\n        <time>%s</time>\n      </trkpt>\n",
                    pt.latitude,
                    pt.longitude,
                    pt.altitude,
                    pointIso
                )
            )
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")

        return sb.toString()
    }

    /**
     * Shares a trail either via text summary or as a formatted GPX XML file.
     */
    fun shareTrailSummary(context: Context, trek: TrekEntity) {
        val points = TrekRepository.decodePoints(trek.encodedTrailPoints)
        val distanceKm = String.format(Locale.US, "%.2f km", trek.distanceMeters / 1000.0)
        val climbM = String.format(Locale.US, "+%.0f m", trek.elevationGainMeters)
        val duration = TrekMetricsEngine.formatDuration(trek.durationSeconds)

        val textPayload = """
🏔️ AROLOCK TRAIL REPORT: ${trek.title}
─────────────────────────────
• Difficulty: ${trek.difficulty}
• Distance: $distanceKm
• Elevation Gain: $climbM
• Duration: $duration
• Calories Burned: ${trek.caloriesKcal} kcal
• Recorded Points: ${points.size} GPS fixes
${if (trek.notes.isNotBlank()) "\nNotes: ${trek.notes}" else ""}

Recorded with arolock — Explore. Record. Remember.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "arolock Trail: ${trek.title}")
            putExtra(Intent.EXTRA_TEXT, textPayload)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Trail Report"))
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
