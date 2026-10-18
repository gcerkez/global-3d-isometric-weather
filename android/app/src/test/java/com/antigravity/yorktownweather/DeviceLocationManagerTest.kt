package com.antigravity.yorktownweather

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DeviceLocationManager distance calculations and diorama auto-matching.
 */
class DeviceLocationManagerTest {

    @Test
    fun testHaversineDistanceZeroForSameCoordinates() {
        val dist = DeviceLocationManager.calculateDistanceMiles(37.2388, -76.5097, 37.2388, -76.5097)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun testHaversineDistanceKnownCities() {
        // Yorktown, VA to Jamestown, VA (~14.7 miles)
        val distYtoJ = DeviceLocationManager.calculateDistanceMiles(37.2388, -76.5097, 37.2085, -76.7741)
        assertTrue("Distance Yorktown to Jamestown should be ~14-16 miles, got $distYtoJ", distYtoJ in 13.0..17.0)

        // London to Paris (~213 miles)
        val distLtoP = DeviceLocationManager.calculateDistanceMiles(51.5074, -0.1278, 48.8566, 2.3522)
        assertTrue("Distance London to Paris should be ~200-230 miles, got $distLtoP", distLtoP in 200.0..230.0)
    }

    @Test
    fun testFindClosestDioramaForYorktownAndTabbVA() {
        // User in Tabb, VA (37.15, -76.45)
        val (dioramaTabb, distTabb) = DeviceLocationManager.findClosestDiorama(37.15, -76.45)
        assertEquals("yorktown", dioramaTabb.id)
        assertTrue(distTabb < 15.0)

        // User directly in Yorktown, VA
        val (dioramaYorktown, distYorktown) = DeviceLocationManager.findClosestDiorama(37.2388, -76.5097)
        assertEquals("yorktown", dioramaYorktown.id)
        assertEquals(0.0, distYorktown, 0.05)
    }

    @Test
    fun testFindClosestDioramaForJamestownVA() {
        val (diorama, dist) = DeviceLocationManager.findClosestDiorama(37.2085, -76.7741)
        assertEquals("jamestown", diorama.id)
        assertEquals(0.0, dist, 0.05)
    }

    @Test
    fun testFindClosestDioramaForMajorRegions() {
        val testCases = listOf(
            Triple(38.8951, -77.0364, "washington_dc"),
            Triple(40.7128, -74.0060, "new_york"),
            Triple(36.8529, -75.9780, "virginia_beach"),
            Triple(37.1667, -76.0833, "chesapeake_bay"),
            Triple(33.6891, -78.8867, "myrtle_beach"),
            Triple(33.7490, -84.3880, "atlanta"),
            Triple(28.5383, -81.3792, "orlando"),
            Triple(30.2672, -97.7431, "texas"),
            Triple(39.7392, -104.9903, "denver"),
            Triple(38.8339, -104.8214, "colorado_springs"),
            Triple(33.4484, -112.0740, "arizona"),
            Triple(36.1699, -115.1398, "las_vegas"),
            Triple(37.7749, -122.4194, "san_francisco"),
            Triple(41.8781, -87.6298, "chicago_great_lakes"),
            Triple(47.6062, -122.3321, "seattle_pnw"),
            Triple(25.7617, -80.1918, "miami_caribbean"),
            Triple(51.5074, -0.1278, "london_uk"),
            Triple(48.8566, 2.3522, "paris_europe"),
            Triple(41.9028, 12.4964, "rome_mediterranean"),
            Triple(30.0444, 31.2357, "cairo_giza"),
            Triple(35.6762, 139.6503, "tokyo_east_asia")
        )

        for ((lat, lon, expectedId) in testCases) {
            val (closest, dist) = DeviceLocationManager.findClosestDiorama(lat, lon)
            assertEquals("Failed for $expectedId", expectedId, closest.id)
            assertTrue("Distance for matching coords should be near 0, got $dist", dist < 1.0)
        }
    }

    @Test
    fun testRegionalDioramaListIntegrity() {
        val list = DeviceLocationManager.REGIONAL_DIORAMAS
        assertEquals("Must have 28 pre-generated dioramas", 28, list.size)

        val ids = mutableSetOf<String>()
        for (d in list) {
            assertTrue("ID must not be empty", d.id.isNotEmpty())
            assertTrue("Duplicate ID found: ${d.id}", ids.add(d.id))
            assertTrue("Name must not be empty", d.name.isNotEmpty())
            assertTrue("Description must not be empty", d.description.isNotEmpty())
            assertTrue("Latitude must be valid (-90..90)", d.lat in -90.0..90.0)
            assertTrue("Longitude must be valid (-180..180)", d.lon in -180.0..180.0)
        }
    }
}
