package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.ArolockDatabase
import com.example.data.TrekEntity
import com.example.data.TrekRepository
import com.example.model.TrailPoint
import com.example.model.Waypoint
import com.example.model.WaypointCategory
import com.example.service.GpxExportService
import com.example.service.LocationService
import com.example.service.TrekMetricsEngine
import com.example.ui.components.ElevationProfileChart
import com.example.ui.dialogs.AddWaypointDialog
import com.example.ui.dialogs.GpxViewerDialog
import com.example.ui.dialogs.SaveTrekSummaryDialog
import com.example.ui.map.OsmMapView
import com.example.ui.map.TrailMapCanvas
import com.example.ui.photos.TrekPhotoSection
import com.example.ui.theme.ArolockTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ArolockTheme {
        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          ArolockHomeScreen(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun ArolockHomeScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val locationService = remember { LocationService(context) }
  val database = remember { ArolockDatabase.getDatabase(context) }
  val repository = remember { TrekRepository(database.trekDao()) }

  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  // Room Data Observation
  val savedTreks by repository.allTreks.collectAsState(initial = emptyList())
  val trekCount by repository.trekCount.collectAsState(initial = 0)
  val totalSavedDistance by repository.totalDistanceMeters.collectAsState(initial = 0.0)
  val totalSavedElevationGain by repository.totalElevationGainMeters.collectAsState(initial = 0.0)

  // Track recorded trail points & waypoints
  val recordedPoints = remember { mutableStateListOf<TrailPoint>() }
  val markedWaypoints = remember { mutableStateListOf<Waypoint>() }

  // Timing states
  var isTrackingActive by remember { mutableStateOf(false) }
  var isTrackingPaused by remember { mutableStateOf(false) }
  var elapsedDurationSeconds by remember { mutableLongStateOf(480L) } // Initial 8 mins demo
  var trekStartTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() - 480_000L) }

  // Dialog states
  var showSaveDialog by remember { mutableStateOf(false) }
  var showAddWaypointDialog by remember { mutableStateOf(false) }
  var viewingGpxTrek by remember { mutableStateOf<TrekEntity?>(null) }
  val activeTrekPhotos = remember { mutableStateListOf<String>() }
  var useOsmStreetMaps by remember { mutableStateOf(true) }

  // User weight for calories calculation
  val userWeightKg = 70.0

  // Permissions state
  fun checkHasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  var hasLocationPermission by remember { mutableStateOf(checkHasLocationPermission()) }
  var permissionDeniedNotice by remember { mutableStateOf(false) }
  var currentPoint by remember { mutableStateOf<TrailPoint?>(null) }
  var gpsSignalStatus by remember { mutableStateOf("Checking GPS...") }
  var trackingJob by remember { mutableStateOf<Job?>(null) }

  // Activity Result launcher for runtime location permission
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

    if (fineGranted || coarseGranted) {
      hasLocationPermission = true
      permissionDeniedNotice = false
      gpsSignalStatus = "Permission Granted. Ready."
      val lastLoc = locationService.getLastKnownLocation()
      if (lastLoc != null) {
        currentPoint = lastLoc
        if (recordedPoints.isEmpty()) {
          recordedPoints.add(lastLoc)
        }
      }
    } else {
      hasLocationPermission = false
      permissionDeniedNotice = true
      gpsSignalStatus = "Location Permission Denied"
    }
  }

  // Pre-seed an authentic mountain trail sequence so user immediately sees live metrics & waypoints
  LaunchedEffect(Unit) {
    if (recordedPoints.isEmpty()) {
      val baseLat = 45.8326
      val baseLon = 6.8652
      val baseAlt = 1040.0
      val samplePoints = listOf(
        TrailPoint(baseLat, baseLon, baseAlt, 1.2f, 3.5f),
        TrailPoint(baseLat + 0.0006, baseLon + 0.0005, baseAlt + 18.0, 1.1f, 3.0f),
        TrailPoint(baseLat + 0.0014, baseLon + 0.0013, baseAlt + 35.0, 1.3f, 3.2f),
        TrailPoint(baseLat + 0.0022, baseLon + 0.0020, baseAlt + 62.0, 0.9f, 2.8f),
        TrailPoint(baseLat + 0.0029, baseLon + 0.0011, baseAlt + 88.0, 1.0f, 3.1f),
        TrailPoint(baseLat + 0.0035, baseLon + 0.0001, baseAlt + 115.0, 1.2f, 2.5f)
      )
      recordedPoints.addAll(samplePoints)
      currentPoint = samplePoints.last()

      // Sample scenic viewpoint waypoint
      if (markedWaypoints.isEmpty()) {
        markedWaypoints.add(
          Waypoint(
            title = "Eagle Peak Ridge",
            category = WaypointCategory.VIEWPOINT,
            latitude = baseLat + 0.0022,
            longitude = baseLon + 0.0020,
            altitude = baseAlt + 62.0,
            notes = "Clear view of Mont Blanc massif."
          )
        )
      }
    }
  }

  // Active Timer Loop
  LaunchedEffect(isTrackingActive, isTrackingPaused) {
    if (isTrackingActive && !isTrackingPaused) {
      while (true) {
        delay(1000L)
        elapsedDurationSeconds += 1
      }
    }
  }

  // Clean up coroutines on dispose
  DisposableEffect(Unit) {
    onDispose {
      trackingJob?.cancel()
    }
  }

  // Compute live metrics dynamically
  val computedStats = remember(recordedPoints.size, elapsedDurationSeconds, userWeightKg) {
    TrekMetricsEngine.computeStats(
      points = recordedPoints.toList(),
      durationSeconds = elapsedDurationSeconds,
      userWeightKg = userWeightKg
    )
  }

  // Save Dialog Pop-up
  if (showSaveDialog) {
    SaveTrekSummaryDialog(
      distanceMeters = computedStats.totalDistanceMeters,
      elevationGainMeters = computedStats.elevationGainMeters,
      elevationLossMeters = computedStats.elevationLossMeters,
      durationSeconds = elapsedDurationSeconds,
      caloriesKcal = computedStats.estimatedCaloriesKcal,
      pointsCount = recordedPoints.size,
      initialPhotoUri = activeTrekPhotos.firstOrNull(),
      onDismiss = { showSaveDialog = false },
      onSaveConfirmed = { title, difficulty, notes, coverPhotoUri ->
        coroutineScope.launch {
          val finalPhoto = coverPhotoUri ?: activeTrekPhotos.firstOrNull()
          val newTrek = TrekEntity(
            title = title,
            difficulty = difficulty,
            notes = notes,
            distanceMeters = computedStats.totalDistanceMeters,
            elevationGainMeters = computedStats.elevationGainMeters,
            elevationLossMeters = computedStats.elevationLossMeters,
            durationSeconds = elapsedDurationSeconds,
            caloriesKcal = computedStats.estimatedCaloriesKcal,
            startTimestamp = trekStartTimestamp,
            endTimestamp = System.currentTimeMillis(),
            encodedTrailPoints = TrekRepository.encodePoints(recordedPoints.toList()),
            photoUri = finalPhoto
          )
          repository.saveTrek(newTrek)
          showSaveDialog = false
          // Reset trek state & active photos
          isTrackingActive = false
          isTrackingPaused = false
          elapsedDurationSeconds = 0L
          activeTrekPhotos.clear()
          gpsSignalStatus = "Trail saved with photos to history!"
        }
      }
    )
  }

  // Add Waypoint Dialog Pop-up
  if (showAddWaypointDialog) {
    AddWaypointDialog(
      currentLocation = currentPoint,
      onDismiss = { showAddWaypointDialog = false },
      onAddWaypoint = { wp ->
        markedWaypoints.add(wp)
        showAddWaypointDialog = false
      }
    )
  }

  // GPX Viewer Dialog Pop-up
  viewingGpxTrek?.let { trekToView ->
    GpxViewerDialog(
      trek = trekToView,
      onDismiss = { viewingGpxTrek = null }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Top Bar / Brand
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(44.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Landscape,
              contentDescription = "arolock logo",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(26.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "arolock",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Explore. Record. Remember.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Surface(
        shape = CircleShape,
        color = if (hasLocationPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(2.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (hasLocationPermission) Icons.Default.GpsFixed else Icons.Default.LocationSearching,
            contentDescription = "GPS Status",
            tint = if (hasLocationPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (hasLocationPermission) "GPS Ready" else "No GPS Perm",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (hasLocationPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }
    }

    // Permission Alert Banner if denied
    AnimatedVisibility(visible = permissionDeniedNotice && !hasLocationPermission) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF382320)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = Color(0xFFFFB4AB),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Location Permission Required",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFFFFB4AB)
            )
            Text(
              text = "arolock requires GPS permissions to record your trek routes and calculate altitude and distance.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD6C2BF)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
              onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                  data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Text("Open App Settings", style = MaterialTheme.typography.labelSmall)
            }
          }
          IconButton(onClick = { permissionDeniedNotice = false }) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Dismiss",
              tint = Color(0xFFD6C2BF),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    // Map Engine Header & Mode Switcher
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Explore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (useOsmStreetMaps) "Live OpenStreetMap" else "Offline Trail Canvas",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Row(modifier = Modifier.padding(2.dp)) {
          FilterChip(
            selected = useOsmStreetMaps,
            onClick = { useOsmStreetMaps = true },
            label = { Text("Map", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
            },
            shape = RoundedCornerShape(10.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          FilterChip(
            selected = !useOsmStreetMaps,
            onClick = { useOsmStreetMaps = false },
            label = { Text("Grid", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Landscape,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
            },
            shape = RoundedCornerShape(10.dp)
          )
        }
      }
    }

    // Interactive Outdoor Trail Map Visualizer with Real World Maps + Waypoint Pins
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF131D18)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp)
        .testTag("interactive_trail_map")
    ) {
      if (useOsmStreetMaps) {
        OsmMapView(
          routePoints = recordedPoints,
          currentLocation = currentPoint,
          waypoints = markedWaypoints,
          isRecording = isTrackingActive && !isTrackingPaused,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        TrailMapCanvas(
          routePoints = recordedPoints,
          currentLocation = currentPoint,
          waypoints = markedWaypoints,
          isRecording = isTrackingActive && !isTrackingPaused,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    // Simulation / Map Control Strip + Add Waypoint Action
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = { showAddWaypointDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(34.dp).testTag("drop_waypoint_button")
      ) {
        Icon(imageVector = Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Pin Landmark", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            val last = currentPoint ?: TrailPoint(45.8326, 6.8652, 1040.0)
            val nextLat = last.latitude + (Random.nextDouble(0.0003, 0.0009))
            val nextLon = last.longitude + (Random.nextDouble(0.0002, 0.0008))
            val nextAlt = last.altitude + (Random.nextDouble(4.0, 12.5))
            val newPt = TrailPoint(nextLat, nextLon, nextAlt, 1.4f, 2.5f)
            recordedPoints.add(newPt)
            currentPoint = newPt
            elapsedDurationSeconds += 25
          },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Climb", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = {
            recordedPoints.clear()
            markedWaypoints.clear()
            val resetPt = TrailPoint(45.8326, 6.8652, 1040.0, 0f, 2.0f)
            recordedPoints.add(resetPt)
            currentPoint = resetPt
            elapsedDurationSeconds = 0L
          },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reset", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    // Step 7: Dynamic Elevation Profile Chart
    ElevationProfileChart(
      points = recordedPoints.toList(),
      modifier = Modifier.fillMaxWidth()
    )

    // Live Trekking Dashboard (Big Outdoor HUD)
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Live Trek Telemetry",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
          ) {
            Text(
              text = TrekMetricsEngine.formatDuration(elapsedDurationSeconds),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
              ),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }

        // Primary Metrics Row 1 (Distance, Gain, Calories)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricTile(
            title = "Distance",
            value = String.format(Locale.US, "%.2f", computedStats.totalDistanceMeters / 1000.0),
            unit = "km",
            icon = Icons.Default.Straighten,
            modifier = Modifier.weight(1f)
          )
          MetricTile(
            title = "Elevation Gain",
            value = String.format(Locale.US, "+%.0f", computedStats.elevationGainMeters),
            unit = "m",
            icon = Icons.Default.TrendingUp,
            modifier = Modifier.weight(1f)
          )
          MetricTile(
            title = "Est. Calories",
            value = computedStats.estimatedCaloriesKcal.toString(),
            unit = "kcal",
            icon = Icons.Default.FitnessCenter,
            modifier = Modifier.weight(1f)
          )
        }

        // Secondary Metrics Row 2 (Current Alt, Avg Speed, Incline/Loss)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricTile(
            title = "Current Alt",
            value = String.format(Locale.US, "%.0f", computedStats.currentElevationMeters),
            unit = "m",
            icon = Icons.Default.Landscape,
            modifier = Modifier.weight(1f)
          )
          MetricTile(
            title = "Avg Speed",
            value = String.format(Locale.US, "%.1f", computedStats.averageSpeedKmh),
            unit = "km/h",
            icon = Icons.Default.Speed,
            modifier = Modifier.weight(1f)
          )
          MetricTile(
            title = "Elevation Loss",
            value = String.format(Locale.US, "-%.0f", computedStats.elevationLossMeters),
            unit = "m",
            icon = Icons.Default.TrendingDown,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Hero Trek Recording Controls (Start / Pause / Resume / Finish)
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("hero_trek_card")
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary,
                Color(0xFF142B1F)
              )
            )
          )
          .padding(24.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column {
              Text(
                text = when {
                  !isTrackingActive -> "READY FOR YOUR ADVENTURE?"
                  isTrackingPaused -> "TREK PAUSED"
                  else -> "TREK RECORDING ACTIVE"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                  letterSpacing = 1.2.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = when {
                  !isTrackingActive -> MaterialTheme.colorScheme.secondaryContainer
                  isTrackingPaused -> Color(0xFFFDE047)
                  else -> Color(0xFF86EFAC)
                }
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = when {
                  !isTrackingActive -> "Start Trek"
                  isTrackingPaused -> "Paused"
                  else -> "Tracking Trail"
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isTrackingActive) Icons.Default.DirectionsWalk else Icons.Default.Navigation,
                  contentDescription = "Navigation Icon",
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
          }

          Text(
            text = when {
              !isTrackingActive -> "Tap below to engage GPS satellite tracking, start the timer, and compute live trek metrics."
              isTrackingPaused -> "Tracking is temporarily paused. Tap RESUME when you continue your hike."
              else -> "Actively streaming GPS coordinates, computing climbing elevation, and estimating calorie expenditure."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
          )

          if (!isTrackingActive) {
            // Big Start Trek Button
            Button(
              onClick = {
                if (!hasLocationPermission) {
                  permissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                  )
                } else {
                  isTrackingActive = true
                  isTrackingPaused = false
                  trekStartTimestamp = System.currentTimeMillis()
                  gpsSignalStatus = "Tracking active"
                  trackingJob = coroutineScope.launch {
                    locationService.getLocationUpdates(minTimeMs = 2000L, minDistanceMeters = 2f)
                      .catch { e -> gpsSignalStatus = "GPS Error: ${e.message}" }
                      .collect { point ->
                        if (!isTrackingPaused) {
                          currentPoint = point
                          recordedPoints.add(point)
                          gpsSignalStatus = "Active fix: ${recordedPoints.size} points"
                        }
                      }
                  }
                }
              },
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
              ),
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("start_trek_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Start Icon",
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (!hasLocationPermission) "GRANT GPS PERMISSION" else "START TREK",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
            }
          } else {
            // Split Action Row: Pause / Resume + Finish Trek
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = { isTrackingPaused = !isTrackingPaused },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isTrackingPaused) MaterialTheme.colorScheme.secondary else Color(0xFFF59E0B),
                  contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = if (isTrackingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isTrackingPaused) "RESUME" else "PAUSE",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
              }

              Button(
                onClick = {
                  trackingJob?.cancel()
                  isTrackingActive = false
                  isTrackingPaused = false
                  showSaveDialog = true // Trigger Save Trail Modal
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFDC2626),
                  contentColor = Color.White
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("finish_trek_button")
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "FINISH",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
              }
            }
          }
        }
      }
    }

    // Trek Photos Section (Upload & View Trek Memories)
    TrekPhotoSection(
      photoUris = activeTrekPhotos.toList(),
      onAddPhotos = { newUris ->
        newUris.forEach { uri ->
          val uriStr = uri.toString()
          if (!activeTrekPhotos.contains(uriStr)) {
            activeTrekPhotos.add(uriStr)
          }
        }
      },
      onRemovePhoto = { uriToRemove ->
        activeTrekPhotos.remove(uriToRemove)
      }
    )

    // Saved Trails History Section
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Saved Trail Memories (${savedTreks.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        if (savedTreks.isNotEmpty()) {
          Text(
            text = "Total: ${String.format(Locale.US, "%.1f km", totalSavedDistance / 1000.0)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      if (savedTreks.isEmpty()) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Landscape,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No saved treks yet",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Tap START TREK above, record some trail steps, and press FINISH to save your first adventure!",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        savedTreks.forEach { trek ->
          SavedTrekCard(
            trek = trek,
            onDelete = {
              coroutineScope.launch {
                repository.deleteTrek(trek)
              }
            },
            onLoadOnMap = {
              val points = TrekRepository.decodePoints(trek.encodedTrailPoints)
              if (points.isNotEmpty()) {
                recordedPoints.clear()
                recordedPoints.addAll(points)
                currentPoint = points.last()
                elapsedDurationSeconds = trek.durationSeconds
              }
            },
            onExportGpx = {
              viewingGpxTrek = trek
            },
            onShare = {
              GpxExportService.shareTrailSummary(context, trek)
            }
          )
        }
      }
    }

    // App Overview & Status Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "arolock Adventure Suite • Production Ready",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = "• Live GPS trail tracking with interactive pan & zoom topographic vector canvas\n" +
              "• Dynamic Elevation Profile graph with ascent curve and peak altitude beacon\n" +
              "• Outdoor telemetry HUD: distance, climbing gain/loss, current alt, speed, calories\n" +
              "• Offline Room persistence for trail logs, GPX export, and waypoint landmark pinning",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "arolock 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Explore. Record. Remember.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Icon(
              imageVector = Icons.Default.Landscape,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun SavedTrekCard(
  trek: TrekEntity,
  onDelete: () -> Unit,
  onLoadOnMap: () -> Unit,
  onExportGpx: () -> Unit,
  onShare: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = trek.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = trek.difficulty,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Text(
              text = TrekMetricsEngine.formatDuration(trek.durationSeconds),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row {
          IconButton(onClick = onShare) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share Trail",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          IconButton(onClick = onDelete) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Trek",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Quick Stats Strip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(String.format(Locale.US, "%.2f km", trek.distanceMeters / 1000.0), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
        Column {
          Text("Elevation Gain", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(String.format(Locale.US, "+%.0f m", trek.elevationGainMeters), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
        Column {
          Text("Calories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${trek.caloriesKcal} kcal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
      }

      if (trek.notes.isNotBlank()) {
        Text(
          text = "\"${trek.notes}\"",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (!trek.photoUri.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
        ) {
          AsyncImage(
            model = trek.photoUri,
            contentDescription = "Trek highlight photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onLoadOnMap,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f).height(36.dp)
        ) {
          Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(Modifier.width(4.dp))
          Text("View on Map", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onExportGpx,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f).height(36.dp)
        ) {
          Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(Modifier.width(4.dp))
          Text("Export GPX", style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

@Composable
fun MetricTile(
  title: String,
  value: String,
  unit: String,
  icon: ImageVector,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1
        )
      }
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = value,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = unit,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

// Maintained for test compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
