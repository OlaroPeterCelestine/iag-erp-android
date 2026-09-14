package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
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
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagCard
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.StatusChip
import africa.iag.erp.android.ui.theme.iagTopBarColors
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.android.ui.theme.toComposeColor
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.formatMoney
import africa.iag.erp.core.reportLines

@Composable
fun ApprovalsScreen(store: ErpStore, onOpenRecord: (String) -> Unit) {
    rememberStoreTick(store)
    val pending = store.appPendingApprovals
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        if (pending.isEmpty()) {
            item { Text("Nothing waiting for your desk.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(pending) { rec ->
                IagCard(modifier = Modifier.padding(bottom = 8.dp), onClick = { onOpenRecord(rec.id) }) {
                    DeskRow(
                        title = rec.title,
                        subtitle = "${rec.entity} · ${rec.subtitle}",
                        icon = Icons.Outlined.AssignmentTurnedIn,
                        status = rec.status,
                    )
                    rec.amount?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(formatMoney(it), fontWeight = FontWeight.Bold)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(module.label) },
                colors = iagTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)) {
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
                    shape = RoundedCornerShape(16.dp),
                )
            }
            item {
                SectionLabel("${module.entities.size} features")
            }
            items(entities, key = { it }) { entity ->
                IagCard(modifier = Modifier.padding(bottom = 8.dp), onClick = { onOpenEntity(entity) }) {
                    DeskRow(
                        title = entity,
                        subtitle = "${store.count(moduleId, entity)} records",
                        icon = Icons.Outlined.GridView,
                        color = module.color.toComposeColor(),
                    )
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(entity) },
                colors = iagTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            if (store.canCreate(moduleId, entity) && entity != "My punches" && entity != "Punch Log" && entity != "Clock In") {
                FloatingActionButton(onClick = onCreate) {
                    Icon(Icons.Outlined.Add, contentDescription = "Create")
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (moduleId == "reports") {
                item { SectionLabel("Snapshot") }
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
                    shape = RoundedCornerShape(16.dp),
                )
            }
            if (rows.isEmpty()) {
                item { Text("No ${entity.lowercase()} yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(rows, key = { it.id }) { rec ->
                IagCard(modifier = Modifier.padding(bottom = 8.dp), onClick = { onOpenRecord(rec.id) }) {
                    DeskRow(
                        title = rec.title,
                        subtitle = rec.subtitle,
                        icon = Icons.Outlined.Description,
                        status = rec.status,
                    )
                    rec.amount?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(formatMoney(it), fontWeight = FontWeight.Bold)
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
