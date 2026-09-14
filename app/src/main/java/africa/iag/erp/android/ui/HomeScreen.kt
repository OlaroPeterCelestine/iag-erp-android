package africa.iag.erp.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.iag.erp.android.ui.theme.IagOrange
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.SearchHit
import africa.iag.erp.core.SearchKind
import africa.iag.erp.core.departmentGroups
import africa.iag.erp.core.featureSummary
import africa.iag.erp.core.formatMoney

@Composable
fun HomeScreen(
    store: ErpStore,
    onOpenDepartment: (String) -> Unit,
    onOpenEntity: (String, String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenClock: () -> Unit,
    onOpenAccess: () -> Unit,
) {
    rememberStoreTick(store)
    var query by remember { mutableStateOf("") }
    val hits = store.searchHits(query)
    val pending = if (store.canApprove) store.pendingApprovals else emptyList()
    val firstName = store.user?.name?.split(" ")?.firstOrNull() ?: "there"
    val searching = query.trim().length >= 2

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Good morning, $firstName", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${store.user?.role ?: "Inspire Africa Group"} · Finance ERP", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (store.canClockIn && !searching) {
            item {
                Card(Modifier.fillMaxWidth().clickable(onClick = onOpenClock)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(if (store.openAttendanceToday() == null) "Clock in" else "Clock out", fontWeight = FontWeight.Bold)
                        Text(
                            "GPS punch against HR Sites and Blocks — every login.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search desks, features, records") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        if (store.isAdmin && !searching) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Access", fontWeight = FontWeight.Bold)
                        Text("Create custom roles, grant apps, then assign people.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onOpenAccess) { Text("Users & custom roles") }
                    }
                }
            }
        }
        if (!searching) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    store.kpis.take(2).forEach { kpi ->
                        Card(Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(kpi.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text(kpi.value, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    store.kpis.drop(2).forEach { kpi ->
                        Card(Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(kpi.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text(kpi.value, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (searching) {
            item { Text("Search", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            if (hits.isEmpty()) {
                item { Text("No matches in desks, features, or records.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(hits, key = { it.id }) { hit ->
                    SearchHitCard(hit, onOpenDepartment, onOpenEntity, onOpenRecord, onOpenTool)
                }
            }
        } else {
            if (store.canApprove) {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        Text("Needs attention", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${pending.size} pending", color = IagOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (pending.isEmpty()) {
                    item { Text("Nothing waiting for your desk.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(pending.take(4)) { rec ->
                        Card(Modifier.fillMaxWidth().clickable { onOpenRecord(rec.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(rec.title, fontWeight = FontWeight.SemiBold)
                                Text("${rec.entity} · ${rec.subtitle}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                rec.amount?.let { Text(formatMoney(it), fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
            departmentGroups.forEach { group ->
                val modules = store.visibleModules.filter { it.group == group }
                if (modules.isNotEmpty()) {
                    item { Text(group, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    items(modules, key = { it.id }) { module ->
                        Card(Modifier.fillMaxWidth().clickable { onOpenDepartment(module.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(module.label, fontWeight = FontWeight.SemiBold)
                                Text(featureSummary(module), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item { Text("Recent", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(store.recent.filter { store.canOpen(it.moduleId) }) { rec ->
                Column(Modifier.fillMaxWidth().clickable { onOpenRecord(rec.id) }.padding(vertical = 8.dp)) {
                    Text(rec.title)
                    Text("${rec.entity} · ${rec.status}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SearchHitCard(
    hit: SearchHit,
    onOpenDepartment: (String) -> Unit,
    onOpenEntity: (String, String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTool: (String) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable {
            when (hit.kind) {
                SearchKind.MODULE -> onOpenDepartment(hit.moduleId)
                SearchKind.ENTITY -> onOpenEntity(hit.moduleId, hit.entity ?: return@clickable)
                SearchKind.RECORD -> onOpenRecord(hit.recordId ?: return@clickable)
                SearchKind.TOOL -> onOpenTool(hit.moduleId)
            }
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(hit.title, fontWeight = FontWeight.SemiBold)
            Text(
                when (hit.kind) {
                    SearchKind.MODULE -> "Desk · ${hit.subtitle}"
                    SearchKind.ENTITY -> "Feature · ${hit.subtitle}"
                    SearchKind.TOOL -> "Workspace · ${hit.subtitle}"
                    SearchKind.RECORD -> hit.subtitle
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}
