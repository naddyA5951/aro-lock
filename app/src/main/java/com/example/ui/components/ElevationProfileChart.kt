package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrailPoint
import java.util.Locale

@Composable
fun ElevationProfileChart(
    points: List<TrailPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("elevation_profile_chart")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val altitudes = points.map { it.altitude }
            val minAlt = altitudes.minOrNull() ?: 0.0
            val maxAlt = altitudes.maxOrNull() ?: 100.0
            val altDiff = (maxAlt - minAlt).coerceAtLeast(10.0)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Elevation Profile",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = String.format(Locale.US, "Min: %.0f m", minAlt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = String.format(Locale.US, "Peak: %.0f m", maxAlt),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF0F1713), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (points.size < 2) {
                        // Flat line placeholder if insufficient points
                        drawLine(
                            color = Color(0xFF22C55E),
                            start = Offset(0f, size.height * 0.7f),
                            end = Offset(size.width, size.height * 0.7f),
                            strokeWidth = 3.dp.toPx()
                        )
                        return@Canvas
                    }

                    val w = size.width
                    val h = size.height
                    val stepX = w / (points.size - 1)

                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { index, pt ->
                        val normY = ((pt.altitude - minAlt) / altDiff).toFloat()
                        val y = h - (normY * (h * 0.8f)) - (h * 0.1f)
                        val x = index * stepX

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        if (index == points.size - 1) {
                            fillPath.lineTo(x, h)
                            fillPath.close()
                        }
                    }

                    // Shaded gradient under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF22C55E).copy(alpha = 0.45f),
                                Color(0xFF22C55E).copy(alpha = 0.05f)
                            )
                        )
                    )

                    // Line stroke
                    drawPath(
                        path = path,
                        color = Color(0xFF4ADE80),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Mark peak point
                    val peakIndex = points.indexOfFirst { it.altitude == maxAlt }.coerceAtLeast(0)
                    val peakX = peakIndex * stepX
                    val peakNormY = ((maxAlt - minAlt) / altDiff).toFloat()
                    val peakY = h - (peakNormY * (h * 0.8f)) - (h * 0.1f)

                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = 4.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )
                }
            }
        }
    }
}
