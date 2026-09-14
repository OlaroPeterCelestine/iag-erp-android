package africa.iag.erp.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagCard
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.StatusChip
import africa.iag.erp.android.ui.theme.iagTopBarColors
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.GeoPoint
import africa.iag.erp.core.HQ_LATITUDE
import africa.iag.erp.core.HQ_LONGITUDE
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Clock In") },
                colors = iagTopBarColors(),
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
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                store.user?.name ?: "Staff",
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
            Text(store.user?.role ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (open == null) {
                    "No open check-in today."
                } else {
                    "Checked in at ${recordField(open, "clockIn", "Clock in")} · ${open.subtitle}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item {
            IagCard {
                if (loc != null) {
                    Text("GPS ${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}")
                    Text("Accuracy ${loc.accuracy.toInt()} m", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val check = verifyAgainstZones(
                        GeoPoint(loc.latitude, loc.longitude),
                        zones,
                        loc.accuracy.toDouble(),
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusChip(check.status)
                    Spacer(Modifier.height(6.dp))
                    Text(check.note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    Text("Waiting for GPS…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        if (!hasLocationPermission(context)) {
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                            return@OutlinedButton
                        }
                        location = lastLocation(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Refresh GPS") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        location = demoHqLocation()
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Use IAG Head Office (demo)") }
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (!hasLocationPermission(context) && location == null) {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                        return@Button
                    }
                    val fix = location ?: lastLocation(context)
                    location = fix
                    if (fix == null) {
                        message = "Capture GPS first, or use the Head Office demo pin."
                        return@Button
                    }
                    val kind = if (open == null) "in" else "out"
                    message = store.punch(kind, fix.latitude, fix.longitude, fix.accuracy.toDouble())
                },
                enabled = store.canClockIn,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (open == null) "Clock in" else "Clock out", fontWeight = FontWeight.SemiBold)
            }
        }
        item { SectionLabel("Sites and blocks") }
        if (zones.isEmpty()) {
            item { Text("HR has no Sites or Blocks with coordinates yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(zones, key = { it.id }) { zone ->
                IagCard {
                    DeskRow(
                        title = zone.name,
                        subtitle = "${zone.kind} · ${zone.radiusMeters.toInt()} m · ${zone.status}",
                        icon = Icons.Outlined.Place,
                        status = zone.status,
                    )
                }
            }
        }
        item { SectionLabel("My punches") }
        if (punches.isEmpty()) {
            item { Text("No attendance rows for this login yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(punches, key = { it.id }) { rec ->
                IagCard(onClick = { onOpenRecord(rec.id) }) {
                    val clockOut = recordField(rec, "clockOut", "Clock out").ifEmpty { "open" }
                    DeskRow(
                        title = rec.title,
                        subtitle = "${recordField(rec, "clockIn", "Clock in")}–$clockOut",
                        icon = Icons.Outlined.Schedule,
                        status = rec.status,
                    )
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

private fun demoHqLocation(): Location = Location("demo").apply {
    latitude = HQ_LATITUDE
    longitude = HQ_LONGITUDE
    accuracy = 12f
}
