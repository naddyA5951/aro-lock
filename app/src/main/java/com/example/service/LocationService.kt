package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.model.TrailPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Native Android LocationService that works offline using GPS_PROVIDER and NETWORK_PROVIDER.
 * Collects periodic GPS tracking points with altitude, speed, and accuracy.
 */
class LocationService(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Checks if device GPS location provider is enabled.
     */
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Obtains the last known location quickly, or null if none is cached.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): TrailPoint? {
        val gpsLoc = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (_: SecurityException) {
            null
        }
        val netLoc = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) {
            null
        }

        val bestLocation = when {
            gpsLoc != null && netLoc != null -> if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
            gpsLoc != null -> gpsLoc
            else -> netLoc
        }

        return bestLocation?.toTrailPoint()
    }

    /**
     * Streams live GPS location updates as a Kotlin Flow.
     * Uses minTimeMs = 2500ms and minDistanceMeters = 3m for battery-efficient trekking tracking.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(minTimeMs: Long = 2500L, minDistanceMeters: Float = 3f): Flow<TrailPoint> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toTrailPoint())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNet = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (hasGps) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceMeters,
                listener,
                Looper.getMainLooper()
            )
        }
        if (hasNet && !hasGps) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTimeMs,
                minDistanceMeters,
                listener,
                Looper.getMainLooper()
            )
        }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun Location.toTrailPoint(): TrailPoint {
        return TrailPoint(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            speed = speed,
            accuracy = accuracy,
            timestamp = time
        )
    }
}
