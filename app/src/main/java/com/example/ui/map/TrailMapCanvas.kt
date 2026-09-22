package com.example.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrailPoint
import com.example.model.Waypoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance, offline-capable adventure trail canvas map.
 * Supports pan, pinch-to-zoom, start/finish markers, live hiker position beacon,
 * custom waypoint landmark pins, and topographic grid lines.
 */
@Composable
fun TrailMapCanvas(
    routePoints: List<TrailPoint>,
    currentLocation: TrailPoint?,
    modifier: Modifier = Modifier,
    waypoints: List<Waypoint> = emptyList(),
    isRecording: Boolean = false,
    onRecenterClicked: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color(0xFF131D18))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            if (canvasWidth == 0f || canvasHeight == 0f) return@Canvas

            // 1. Draw Outdoor Topographic / Coordinate Grid
            val baseGridSpacing = 60.dp.toPx() * scale
            val gridColor = Color(0x1A6FA585)

            var x = (offsetX % baseGridSpacing)
            while (x < canvasWidth) {
                if (x >= 0) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 1f
                    )
                }
                x += baseGridSpacing
            }

            var y = (offsetY % baseGridSpacing)
            while (y < canvasHeight) {
                if (y >= 0) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                }
                y += baseGridSpacing
            }

            // Reference coordinates calculation
            val allPts = if (routePoints.isNotEmpty()) routePoints else listOfNotNull(currentLocation)
            if (allPts.isEmpty()) return@Canvas

            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE

            for (p in allPts) {
                minLat = min(minLat, p.latitude)
                maxLat = max(maxLat, p.latitude)
                minLon = min(minLon, p.longitude)
                maxLon = max(maxLon, p.longitude)
            }

            val latSpan = max(maxLat - minLat, 0.001)
            val lonSpan = max(maxLon - minLon, 0.001)
            val centerLat = (minLat + maxLat) / 2.0
            val centerLon = (minLon + maxLon) / 2.0
            val cosLat = cos(Math.toRadians(centerLat)).toFloat().coerceAtLeast(0.1f)

            val baseZoom = min(
                (canvasWidth * 0.65f) / (lonSpan.toFloat() * cosLat),
                (canvasHeight * 0.65f) / latSpan.toFloat()
            ) * scale

            val centerX = canvasWidth / 2f + offsetX
            val centerY = canvasHeight / 2f + offsetY

            fun toOffset(lat: Double, lon: Double): Offset {
                val dx = (lon - centerLon).toFloat() * cosLat * baseZoom
                val dy = -(lat - centerLat).toFloat() * baseZoom
                return Offset(centerX + dx, centerY + dy)
            }

            // 2. Draw Trail Polyline (Adventure Glowing Path)
            if (routePoints.size >= 2) {
                val trailPath = Path()
                val startScreen = toOffset(routePoints[0].latitude, routePoints[0].longitude)
                trailPath.moveTo(startScreen.x, startScreen.y)

                for (i in 1 until routePoints.size) {
                    val screenPt = toOffset(routePoints[i].latitude, routePoints[i].longitude)
                    trailPath.lineTo(screenPt.x, screenPt.y)
                }

                // Outer Trail Glow
                drawPath(
                    path = trailPath,
                    color = Color(0x3322C55E),
                    style = Stroke(
                        width = 10.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Core Crisp Forest-Green Trail Line
                drawPath(
                    path = trailPath,
                    color = Color(0xFF22C55E),
                    style = Stroke(
                        width = 3.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Start Marker (Hiker Trailhead Flag)
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 7.dp.toPx(),
                    center = startScreen
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = startScreen
                )
            }

            // 3. Draw Custom Waypoint Pins
            for (wp in waypoints) {
                val wpOffset = toOffset(wp.latitude, wp.longitude)
                // Outer ring
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = 8.dp.toPx(),
                    center = wpOffset
                )
                // Center
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = wpOffset
                )
            }

            // 4. Draw Current Position (Blue GPS Beacon with Pulse)
            val current = currentLocation ?: routePoints.lastOrNull()
            if (current != null) {
                val hikerPos = toOffset(current.latitude, current.longitude)

                // Pulse ring
                drawCircle(
                    color = Color(0x2E0EA5E9),
                    radius = 18.dp.toPx(),
                    center = hikerPos
                )
                drawCircle(
                    color = Color(0x550EA5E9),
                    radius = 11.dp.toPx(),
                    center = hikerPos
                )

                // Core GPS Location Dot
                drawCircle(
                    color = Color(0xFF0EA5E9),
                    radius = 6.dp.toPx(),
                    center = hikerPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = hikerPos
                )
            }
        }

        // Map Overlay Badges & Controls
        // Top Info Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC111814),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isRecording) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRecording) "RECORDING TRAIL" else "TOPOGRAPHIC MAP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color(0xFFE2E8F0)
                )
                if (routePoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${routePoints.size} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                if (waypoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${waypoints.size} pins",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFDE047)
                    )
                }
            }
        }

        // Map Zoom Controls & Recenter Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xCC18221C),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Column {
                    IconButton(
                        onClick = { scale = (scale * 1.25f).coerceAtMost(6.0f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = {
                        scale = 1.0f
                        offsetX = 0f
                        offsetY = 0f
                        onRecenterClicked()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
