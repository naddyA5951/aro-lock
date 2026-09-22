package com.example.ui.map

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.TrailPoint
import com.example.model.Waypoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * OpenStreetMap (OSM) Interactive Map component.
 * Free, open-source, global street & topographic maps with zero API keys required.
 */
@Composable
fun OsmMapView(
    routePoints: List<TrailPoint>,
    currentLocation: TrailPoint?,
    modifier: Modifier = Modifier,
    waypoints: List<Waypoint> = emptyList(),
    isRecording: Boolean = false,
    onRecenterClicked: () -> Unit = {}
) {
    val context = LocalContext.current

    // Configure OSM User-Agent and storage cache
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK) // Global OpenStreetMap tiles
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.0)
            val defaultGeo = if (currentLocation != null) {
                GeoPoint(currentLocation.latitude, currentLocation.longitude)
            } else {
                GeoPoint(37.7749, -122.4194)
            }
            controller.setCenter(defaultGeo)
        }
    }

    // Clean up lifecycle
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Update trail polyline and markers when coordinates change
    LaunchedEffect(routePoints, currentLocation, waypoints) {
        mapView.overlays.clear()

        // 1. Trail GPS Polyline
        if (routePoints.isNotEmpty()) {
            val polyline = Polyline(mapView).apply {
                outlinePaint.color = android.graphics.Color.parseColor("#34D399") // Emerald hiking trail
                outlinePaint.strokeWidth = 10f
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            }
            val geoPoints = routePoints.map { GeoPoint(it.latitude, it.longitude) }
            polyline.setPoints(geoPoints)
            mapView.overlays.add(polyline)

            // Start marker
            val startPt = routePoints.first()
            val startMarker = Marker(mapView).apply {
                position = GeoPoint(startPt.latitude, startPt.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Trailhead (Start)"
            }
            mapView.overlays.add(startMarker)
        }

        // 2. Waypoints markers
        waypoints.forEach { wp ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(wp.latitude, wp.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "${wp.title} (${wp.category.label})"
                snippet = wp.notes.ifEmpty { "Altitude: ${wp.altitude.toInt()}m" }
            }
            mapView.overlays.add(marker)
        }

        // 3. Current Live Location Marker
        if (currentLocation != null) {
            val currGeo = GeoPoint(currentLocation.latitude, currentLocation.longitude)
            val currMarker = Marker(mapView).apply {
                position = currGeo
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Current Position"
            }
            mapView.overlays.add(currMarker)
            if (isRecording) {
                mapView.controller.animateTo(currGeo)
            }
        }

        mapView.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Map Controls: Zoom + Center
        Surface(
            shape = CircleShape,
            color = Color(0xDD1B2720),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Column {
                IconButton(
                    onClick = { mapView.controller.zoomIn() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { mapView.controller.zoomOut() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (currentLocation != null) {
                            mapView.controller.animateTo(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                        } else if (routePoints.isNotEmpty()) {
                            mapView.controller.animateTo(GeoPoint(routePoints.last().latitude, routePoints.last().longitude))
                        }
                        onRecenterClicked()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on Live Location",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
