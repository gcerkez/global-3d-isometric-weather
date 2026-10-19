package com.antigravity.yorktownweather

import android.Manifest
import android.app.KeyguardManager
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
     * 2. If no recent location (< 15 mins) is available and screen is unlocked, requests a fresh one-shot fix.
     * 3. When screen is locked, immediately returns the best cached physical location to avoid timeout.
     * 4. Falls back to IP geolocation ONLY if explicitly allowed and no GPS fix is available.
     *    Background widget updates MUST NOT fall back to IP to prevent cellular gateways in Washington D.C.
     *    from overwriting the phone's true physical location.
     */
    suspend fun getBestDeviceLocation(context: Context, allowIpFallback: Boolean = false): Pair<Double, Double>? {
        if (hasLocationPermission(context)) {
            val gpsLoc = getGpsOrNetworkLocation(context)
            if (gpsLoc != null) {
                return Pair(gpsLoc.latitude, gpsLoc.longitude)
            }
        }
        return if (allowIpFallback) fetchIpLocation() else null
    }

    private suspend fun getGpsOrNetworkLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

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

        // When screen is locked, Android limits GPS hardware power.
        // Return best last known physical location immediately to prevent timeout.
        if (km?.isDeviceLocked == true && bestLast != null) {
            return bestLast
        }

        val now = System.currentTimeMillis()
        // If last known location is less than 15 minutes old, return it immediately
        if (bestLast != null && (now - bestLast.time) < 15 * 60 * 1000L) {
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
            "Virginia" to "VA", "Maryland" to "MD", "North Carolina" to "NC", "South Carolina" to "SC",
            "District of Columbia" to "DC", "California" to "CA", "New York" to "NY", "Florida" to "FL",
            "Texas" to "TX", "Illinois" to "IL", "Washington" to "WA", "Pennsylvania" to "PA",
            "Georgia" to "GA", "Colorado" to "CO", "Arizona" to "AZ", "Nevada" to "NV"
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

    data class DioramaLocation(
        val id: String,
        val name: String,
        val lat: Double,
        val lon: Double,
        val description: String
    )

    val REGIONAL_DIORAMAS = listOf(
        DioramaLocation("yorktown", "Historic Yorktown, VA", 37.2388, -76.5097, "1781 Colonial Waterfront, Victory Monument & Coleman Bridge"),
        DioramaLocation("jamestown", "Historic Jamestown, VA", 37.2085, -76.7741, "1607 James Fort Wooden Palisade & 3 Tall Ships on James River"),
        DioramaLocation("virginia_beach", "Virginia Beach, VA", 36.8529, -75.9780, "Oceanfront Boardwalk, Cape Henry Lighthouse & Fishing Pier"),
        DioramaLocation("norfolk", "Norfolk & Hampton Roads, VA", 36.8508, -76.2859, "Waterside District, Nauticus Battleship & Downtown Harborfront"),
        DioramaLocation("chesapeake_bay", "Chesapeake Bay, VA", 37.1667, -76.0833, "Chesapeake Bay Bridge-Tunnel, Oyster/Crab Docks & Harbor Light"),
        DioramaLocation("washington_dc", "Washington, D.C. & Mid-Atlantic", 38.8951, -77.0364, "U.S. Capitol Dome, Washington Monument & Tidal Basin"),
        DioramaLocation("new_york", "New York City & Northeast", 40.7128, -74.0060, "Statue of Liberty, Brooklyn Bridge & Empire State Building"),
        DioramaLocation("myrtle_beach", "Myrtle Beach & Grand Strand, SC", 33.6891, -78.8867, "SkyWheel Ferris Wheel, Oceanfront Promenade & Fishing Pier"),
        DioramaLocation("atlanta", "Atlanta & Georgia Piedmont", 33.7490, -84.3880, "Peachtree Center Tower, Georgia Capitol Dome & Olympic Park"),
        DioramaLocation("orlando", "Orlando & Theme Parks, FL", 28.5383, -81.3792, "Fairytale Spires, Roller Coaster & Lake Eola Swan Lagoon"),
        DioramaLocation("miami_caribbean", "Miami Beach & Florida Keys", 25.7617, -80.1918, "Art Deco Ocean Drive, Overseas Bridge & Coral Reef"),
        DioramaLocation("new_orleans", "New Orleans & Gulf South", 29.9511, -90.0715, "St. Louis Cathedral, French Quarter & Steamboat"),
        DioramaLocation("texas", "Texas Capitol & Hill Country", 30.2672, -97.7431, "Pink Granite Capitol Dome, Austin Skyline & River Walk"),
        DioramaLocation("chicago_great_lakes", "Chicago & Great Lakes Midwest", 41.8781, -87.6298, "Willis Tower, Red Bascule Bridge & Cloud Gate Bean"),
        DioramaLocation("denver", "Denver Mile High Skyline, CO", 39.7392, -104.9903, "Mile High Skyline, Historic Union Station & Rocky Mountain Foothills"),
        DioramaLocation("colorado_springs", "Colorado Springs & Pikes Peak, CO", 38.8339, -104.8214, "Garden of the Gods Sandstone Spires, Pikes Peak Depot & Manor"),
        DioramaLocation("arizona", "Arizona & Sonoran Desert", 33.4484, -112.0740, "Sedona Cathedral Rock, Adobe Mission & Sonoran Oasis"),
        DioramaLocation("grand_canyon", "Grand Canyon & American Southwest", 36.0544, -112.1401, "Red-Rock Buttes, Cliff Dwellings & Colorado River"),
        DioramaLocation("las_vegas", "Las Vegas Strip & Mojave Oasis, NV", 36.1699, -115.1398, "Strip Mega-Resorts, High Roller Wheel & Bellagio Fountains"),
        DioramaLocation("san_francisco", "San Francisco & California Coast", 37.7749, -122.4194, "Golden Gate Bridge, Alcatraz & Painted Ladies"),
        DioramaLocation("seattle_pnw", "Seattle & Pacific Northwest", 47.6062, -122.3321, "Space Needle, Pike Place Market & Mt. Rainier"),
        DioramaLocation("coastal", "New England & Maritime Atlantic", 43.6615, -70.2553, "Striped Lighthouse, Stone Pier & Historic Wharf"),
        DioramaLocation("london_uk", "London & British Isles", 51.5074, -0.1278, "Big Ben, Palace of Westminster & Tower of London"),
        DioramaLocation("paris_europe", "Paris & Western Europe", 48.8566, 2.3522, "Notre-Dame Cathedral, Louvre Palace & Arc de Triomphe"),
        DioramaLocation("rome_mediterranean", "Rome, Athens & Mediterranean", 41.9028, 12.4964, "Colosseum, Parthenon Temple & Roman Aqueduct"),
        DioramaLocation("cairo_giza", "Cairo, Giza & Middle East", 30.0444, 31.2357, "Ancient Sandstone Citadel, Temple Pool & Nile River"),
        DioramaLocation("tokyo_east_asia", "Tokyo, Kyoto & East Asia", 35.6762, 139.6503, "5-Story Pagoda, Imperial Palace Hall & Garden Bridge"),
        DioramaLocation("modern", "Global Metropolis (Sydney / Modern)", -33.8688, 151.2093, "Contemporary Glass Skyscraper District & Harbour Bridge"),
        DioramaLocation("alpine", "Alpine Mountains & Lake Village", 46.5197, 6.6323, "Snow-Capped Peak, Timber Chalets & Turquoise Glacial Lake")
    )

    fun calculateDistanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3958.8 // Earth radius in miles
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    fun findClosestDiorama(lat: Double, lon: Double): Pair<DioramaLocation, Double> {
        var best = REGIONAL_DIORAMAS[0]
        var minD = Double.MAX_VALUE
        for (d in REGIONAL_DIORAMAS) {
            val dist = calculateDistanceMiles(lat, lon, d.lat, d.lon)
            if (dist < minD) {
                minD = dist
                best = d
            }
        }
        return Pair(best, minD)
    }
}
