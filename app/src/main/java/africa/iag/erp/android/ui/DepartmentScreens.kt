package africa.iag.erp.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.android.ui.theme.statusColor
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.formatMoney
import africa.iag.erp.core.reportLines

@Composable
fun ApprovalsScreen(store: ErpStore, onOpenRecord: (String) -> Unit) {
    rememberStoreTick(store)
    val pending = store.pendingApprovals
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        if (pending.isEmpty()) {
            item { Text("Nothing waiting for your desk.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(pending) { rec ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenRecord(rec.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rec.title, fontWeight = FontWeight.SemiBold)
                        Text("${rec.entity} · ${rec.subtitle}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec.status, color = statusColor(rec.status))
                        rec.amount?.let { Text(formatMoney(it), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentScreen(
    store: ErpStore,
    moduleId: String,
    onBack: () -> Unit,
    onOpenEntity: (String) -> Unit,
) {
    rememberStoreTick(store)
    val module = store.moduleById(moduleId)
    if (module == null || !store.canOpen(moduleId)) {
        Column(Modifier.padding(24.dp)) {
            Text("Access denied", fontWeight = FontWeight.Bold)
            Text("Your role cannot open this department.")
        }
        return
    }
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val entities = module.entities.filter { q.isEmpty() || it.lowercase().contains(q) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            item {
                Text(module.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Find a feature") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    singleLine = true,
                )
            }
            item {
                Text("${module.entities.size} features", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(entities, key = { it }) { entity ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenEntity(entity) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(entity, fontWeight = FontWeight.SemiBold)
                        Text("${store.count(moduleId, entity)} records", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityListScreen(
    store: ErpStore,
    moduleId: String,
    entity: String,
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit,
    onCreate: () -> Unit,
) {
    rememberStoreTick(store)
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val rows = store.recordsFor(moduleId, entity).filter {
        q.isEmpty() || it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q) || it.status.lowercase().contains(q)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entity) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            if (store.canCreate(moduleId, entity) && entity != "My punches" && entity != "Punch Log" && entity != "Clock In") {
                FloatingActionButton(onClick = onCreate) { Text("+") }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            if (moduleId == "reports") {
                item { Text("Snapshot", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }
                items(reportLines(entity)) { line ->
                    RowLine(line.first, line.second)
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Filter records") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    singleLine = true,
                )
            }
            if (rows.isEmpty()) {
                item { Text("No ${entity.lowercase()} yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(rows, key = { it.id }) { rec ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenRecord(rec.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rec.title, fontWeight = FontWeight.SemiBold)
                        Text(rec.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec.status, color = statusColor(rec.status))
                        rec.amount?.let { Text(formatMoney(it), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
