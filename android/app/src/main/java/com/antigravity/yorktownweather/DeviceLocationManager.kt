package com.antigravity.yorktownweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Handles device GPS/network location acquisition and reverse-geocoding.
 * Ensures the Android app and Homescreen widget resolve the phone's true physical location
 * (e.g. Yorktown, VA) instead of cellular IP gateway locations (e.g. Washington, D.C.).
 */
object DeviceLocationManager {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Resolves the best available device location.
     * 1. Checks GPS and Network providers via Android LocationManager.
     * 2. If no recent location (< 15 mins) is available, requests a fresh one-shot fix.
     * 3. Falls back to IP geolocation only if permissions or hardware GPS are unavailable.
     */
    suspend fun getBestDeviceLocation(context: Context): Pair<Double, Double>? {
        if (hasLocationPermission(context)) {
            val gpsLoc = getGpsOrNetworkLocation(context)
            if (gpsLoc != null) {
                return Pair(gpsLoc.latitude, gpsLoc.longitude)
            }
        }
        // Fallback to IP geolocation if GPS is unavailable
        return fetchIpLocation()
    }

    private suspend fun getGpsOrNetworkLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var bestLast: Location? = null
        for (p in providers) {
            try {
                if (lm.isProviderEnabled(p)) {
                    val loc = lm.getLastKnownLocation(p)
                    if (loc != null) {
                        if (bestLast == null || loc.time > bestLast.time) {
                            bestLast = loc
                        }
                    }
                }
            } catch (_: SecurityException) {}
        }

        val now = System.currentTimeMillis()
        // If last known location is less than 10 minutes old, return it immediately
        if (bestLast != null && (now - bestLast.time) < 10 * 60 * 1000L) {
            return bestLast
        }

        // Request a fresh one-shot location fix with a 6-second timeout
        val fresh = withTimeoutOrNull(6000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try { lm.removeUpdates(this) } catch (_: Exception) {}
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                var requested = false
                try {
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
                        requested = true
                    }
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
                        requested = true
                    }
                } catch (_: SecurityException) {}

                if (!requested) {
                    if (cont.isActive) cont.resume(bestLast)
                }

                cont.invokeOnCancellation {
                    try { lm.removeUpdates(listener) } catch (_: Exception) {}
                }
            }
        }

        return fresh ?: bestLast
    }

    /**
     * Reverse geocodes coordinates to a clean human-readable city string (e.g. "Yorktown, VA").
     */
    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        // 1. Try Android built-in Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val a = addresses[0]
                    val city = a.locality ?: a.subLocality ?: a.subAdminArea ?: a.adminArea
                    val state = a.adminArea
                    if (!city.isNullOrBlank()) {
                        val stateAbbr = getStateAbbr(state)
                        return@withContext if (stateAbbr.isNotEmpty()) "$city, $stateAbbr" else city
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback to BigDataCloud reverse geocode API (accurate and returns county/city/state)
        try {
            val url = URL("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=en")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val city = json.optString("city", "").ifEmpty { json.optString("locality", "") }.trim()
            val state = json.optString("principalSubdivision", "").trim()
            val code = json.optString("principalSubdivisionCode", "").trim()
            val stateStr = if (code.contains("-")) code.substringAfter("-") else state
            if (city.isNotEmpty()) {
                return@withContext if (stateStr.isNotEmpty()) "$city, $stateStr" else city
            }
        } catch (_: Exception) {}

        return@withContext String.format(Locale.US, "%.3f, %.3f", lat, lon)
    }

    private fun getStateAbbr(state: String?): String {
        if (state.isNullOrBlank()) return ""
        if (state.length == 2) return state.uppercase()
        val map = mapOf(
            "Virginia" to "VA", "Maryland" to "MD", "North Carolina" to "NC", "District of Columbia" to "DC",
            "California" to "CA", "New York" to "NY", "Florida" to "FL", "Texas" to "TX",
            "Illinois" to "IL", "Washington" to "WA", "Pennsylvania" to "PA", "Georgia" to "GA"
        )
        return map[state] ?: state
    }

    private fun fetchIpLocation(): Pair<Double, Double>? {
        return runCatching {
            val conn = (URL("https://get.geojs.io/v1/ip/geo.json").openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val lat = json.optDouble("latitude", Double.NaN)
            val lon = json.optDouble("longitude", Double.NaN)
            if (!lat.isNaN() && !lon.isNaN()) Pair(lat, lon) else null
        }.getOrNull()
    }
}
