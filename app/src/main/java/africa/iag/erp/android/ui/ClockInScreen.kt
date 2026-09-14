package africa.iag.erp.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.GeoPoint
import africa.iag.erp.core.recordField
import africa.iag.erp.core.verifyAgainstZones

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockInScreen(
    store: ErpStore,
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clock In") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        ClockInPanel(store, onOpenRecord, Modifier.padding(padding))
    }
}

@Composable
fun ClockInPanel(
    store: ErpStore,
    onOpenRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    rememberStoreTick(store)
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf<Location?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            location = lastLocation(context)
            if (location == null) {
                message = "GPS is on, but no fix yet. Walk outside and try again."
            }
        } else {
            message = "Location permission is required to clock in."
        }
    }

    val open = store.openAttendanceToday()
    val zones = store.geofenceZones()
    val punches = store.myPunches()
    val loc = location ?: lastLocation(context)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "GPS is checked against HR Sites and Blocks. Outside the fence is rejected and still logged.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (open == null) {
                            "No open check-in today."
                        } else {
                            "Checked in at ${recordField(open, "clockIn", "Clock in")} · ${open.subtitle}"
                        },
                    )
                    if (loc != null) {
                        Text("Latitude ${"%.6f".format(loc.latitude)}")
                        Text("Longitude ${"%.6f".format(loc.longitude)}")
                        Text("Accuracy ${loc.accuracy.toInt()} m")
                        val check = verifyAgainstZones(
                            GeoPoint(loc.latitude, loc.longitude),
                            zones,
                            loc.accuracy.toDouble(),
                        )
                        Text("Fence ${check.status}")
                        Text(check.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Waiting for GPS…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (!hasLocationPermission(context)) {
                                launcher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                                return@Button
                            }
                            val fix = lastLocation(context)
                            location = fix
                            if (fix == null) {
                                message = "No GPS fix yet. Walk outside and try again."
                                return@Button
                            }
                            val kind = if (open == null) "in" else "out"
                            message = store.punch(kind, fix.latitude, fix.longitude, fix.accuracy.toDouble())
                        },
                        enabled = store.canClockIn,
                    ) {
                        Text(if (open == null) "Clock in" else "Clock out")
                    }
                    message?.let { Text(it) }
                }
            }
        }
        item { Text("Sites and blocks", fontWeight = FontWeight.Bold) }
        if (zones.isEmpty()) {
            item { Text("HR has no Sites or Blocks with coordinates yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(zones, key = { it.id }) { zone ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(zone.name, fontWeight = FontWeight.SemiBold)
                        Text("${zone.kind} · ${zone.radiusMeters.toInt()} m · ${zone.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Text("My punches", fontWeight = FontWeight.Bold) }
        if (punches.isEmpty()) {
            item { Text("No attendance rows for this login yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(punches, key = { it.id }) { rec ->
                Card(Modifier.fillMaxWidth().clickable { onOpenRecord(rec.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rec.title, fontWeight = FontWeight.SemiBold)
                        val clockOut = recordField(rec, "clockOut", "Clock out").ifEmpty { "open" }
                        Text(
                            "${recordField(rec, "clockIn", "Clock in")}–$clockOut · ${rec.status}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private fun lastLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.FUSED_PROVIDER)
    return providers.mapNotNull { provider ->
        try {
            manager.getLastKnownLocation(provider)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }.maxByOrNull { it.time }
}
