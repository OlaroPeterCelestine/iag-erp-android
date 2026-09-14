package africa.iag.erp.core

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

data class GeofenceZone(
    val id: String,
    val name: String,
    val kind: String,
    val siteName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val status: String,
)

data class GeofenceCheck(
    val ok: Boolean,
    val status: String,
    val distanceMeters: Int,
    val zone: GeofenceZone?,
    val note: String,
)

const val HQ_LATITUDE = 0.347596
const val HQ_LONGITUDE = 32.582520
const val ACP_LATITUDE = -0.341111
const val ACP_LONGITUDE = 31.736111

private const val EARTH_RADIUS_M = 6_371_000.0

fun parseCoord(value: String?): Double? {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val n = raw.toDoubleOrNull() ?: return null
    return if (n.isFinite()) n else null
}

fun recordField(record: ErpRecord, vararg keys: String): String {
    for (key in keys) {
        val value = record.fields[key]?.trim().orEmpty()
        if (value.isNotEmpty()) return value
    }
    return ""
}

fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
    fun rad(deg: Double) = deg * Math.PI / 180
    val dLat = rad(b.latitude - a.latitude)
    val dLon = rad(b.longitude - a.longitude)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(rad(a.latitude)) * cos(rad(b.latitude)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
}

fun zoneFromRecord(record: ErpRecord, kind: String): GeofenceZone? {
    val lat = parseCoord(recordField(record, "latitude", "Latitude")) ?: return null
    val lng = parseCoord(recordField(record, "longitude", "Longitude")) ?: return null
    val radius = parseCoord(recordField(record, "radiusMeters", "Radius (m)", "Radius")) ?: 100.0
    return GeofenceZone(
        id = record.id,
        name = record.title,
        kind = kind,
        siteName = if (kind == "block") record.subtitle.ifEmpty { record.title } else record.title,
        latitude = lat,
        longitude = lng,
        radiusMeters = maxOf(10.0, radius),
        status = record.status,
    )
}

fun verifyAgainstZones(point: GeoPoint, zones: List<GeofenceZone>, accuracyMeters: Double = 0.0): GeofenceCheck {
    val active = zones.filter { it.status.lowercase() !in setOf("inactive", "disabled", "archived", "closed") }
    if (active.isEmpty()) {
        return GeofenceCheck(false, "Outside", Int.MAX_VALUE, null, "No active sites or blocks with coordinates are configured.")
    }
    var bestZone = active.first()
    var bestDistance = haversineMeters(point, GeoPoint(bestZone.latitude, bestZone.longitude))
    for (zone in active.drop(1)) {
        val distance = haversineMeters(point, GeoPoint(zone.latitude, zone.longitude))
        if (distance < bestDistance) {
            bestZone = zone
            bestDistance = distance
        }
    }
    val distanceMeters = bestDistance.toInt()
    val effective = maxOf(0.0, distanceMeters - minOf(accuracyMeters, 50.0))
    if (effective <= bestZone.radiusMeters) {
        return GeofenceCheck(true, "Verified", distanceMeters, bestZone, "Inside ${bestZone.name} geofence ($distanceMeters m of ${bestZone.radiusMeters.toInt()} m radius).")
    }
    if (effective <= bestZone.radiusMeters * 1.5) {
        return GeofenceCheck(false, "Flagged", distanceMeters, bestZone, "Near ${bestZone.name} but outside the ${bestZone.radiusMeters.toInt()} m radius ($distanceMeters m away).")
    }
    return GeofenceCheck(false, "Outside", distanceMeters, bestZone, "Outside all geofences. Nearest: ${bestZone.name} at $distanceMeters m.")
}

fun nowClock(): String {
    val cal = java.util.Calendar.getInstance()
    return "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
}

fun hoursBetween(clockIn: String, clockOut: String): String {
    fun minutes(value: String): Int {
        val parts = value.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return 0
        return parts[0] * 60 + parts[1]
    }
    val mins = maxOf(0, minutes(clockOut) - minutes(clockIn))
    return "%.2f".format(mins / 60.0)
}

val webErpDepartmentIds: List<String> = listOf(
    "banking", "receipts-payments", "expense-claims", "general-requests", "oral-payment-requests",
    "sales", "purchases", "inventory", "projects", "contract-manager", "fleet", "security", "crm",
    "logistics", "distribution", "rnd", "lab", "qa", "production", "benchmark", "pos", "payroll",
    "investments", "assets", "capital", "accounts", "folders", "documents", "reports",
)
