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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Widgets
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagGroupedCard
import africa.iag.erp.android.ui.theme.IagListRow
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.iagTopBarColors
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.APP_NAME
import africa.iag.erp.core.APP_VERSION
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.departmentGroups
import africa.iag.erp.core.workspaceGroups
import africa.iag.erp.core.workspaceTools

@Composable
fun MoreScreen(
    store: ErpStore,
    onOpenTool: (String) -> Unit,
    onOpenAccess: () -> Unit,
    onOpenProfile: () -> Unit,
    onSwitchApp: () -> Unit = {},
) {
    rememberStoreTick(store)
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        if (store.activeAppId != null) {
            item { SectionLabel("Apps") }
            item {
                IagGroupedCard {
                    IagListRow(onClick = onSwitchApp, divider = false) {
                        DeskRow(
                            title = "Switch app",
                            subtitle = store.activeSuiteApp?.let { "You are in ${it.label}." } ?: "Open another IAG tool.",
                            icon = Icons.Outlined.Apps,
                        )
                    }
                }
            }
        }
        workspaceGroups.forEach { group ->
            val tools = store.visibleWorkspaceTools.filter { it.group == group }
            if (tools.isNotEmpty()) {
                item {
                    SectionLabel(group)
                    IagGroupedCard {
                        tools.forEachIndexed { index, tool ->
                            IagListRow(onClick = { onOpenTool(tool.id) }, divider = index < tools.lastIndex) {
                                DeskRow(
                                    title = tool.label,
                                    subtitle = tool.description,
                                    icon = Icons.Outlined.Widgets,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { SectionLabel("Account") }
        item {
            IagGroupedCard {
                IagListRow(onClick = onOpenProfile, divider = store.isAdmin) {
                    DeskRow(title = "Profile", subtitle = "Name, theme, and sign out", icon = Icons.Outlined.Person)
                }
                if (store.isAdmin) {
                    IagListRow(onClick = onOpenAccess, divider = false) {
                        DeskRow(title = "Users & roles", subtitle = "Custom roles and workspace users", icon = Icons.Outlined.Shield)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceToolScreen(
    store: ErpStore,
    toolId: String,
    onBack: () -> Unit,
    onOpenDepartment: (String) -> Unit,
    onOpenEntity: (String, String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTool: (String) -> Unit,
) {
    rememberStoreTick(store)
    val tool = workspaceTools.firstOrNull { it.id == toolId }
    var query by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tool?.label ?: "Workspace") },
                colors = iagTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (toolId == "clock-in") {
            ClockInPanel(store, onOpenRecord, Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            when (toolId) {
                "trace" -> {
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Look up a document, plate, lot, or person") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                        )
                    }
                    val hits = store.searchHits(query)
                    if (query.trim().length < 2) {
                        item { Text("Type at least two characters.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else if (hits.isEmpty()) {
                        item { Text("Nothing matched.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(hits, key = { it.id }) { hit ->
                            SearchHitCard(hit, onOpenDepartment, onOpenEntity, onOpenRecord, onOpenTool)
                        }
                    }
                }
                "analytics" -> {
                    item { Stat("Departments", "${store.visibleModules.size}") }
                    item { Stat("Features", "${store.visibleModules.sumOf { it.entities.size }}") }
                    item { Stat("Records", "${store.records.count { store.canOpen(it.moduleId) }}") }
                    item { Stat("Pending approvals", "${store.pendingApprovals.size}") }
                    departmentGroups.forEach { group ->
                        val modules = store.visibleModules.filter { it.group == group }
                        if (modules.isNotEmpty()) {
                            item { Text(group, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) }
                            items(modules, key = { it.id }) { module ->
                                Stat(module.label, "${store.forModule(module.id).size}")
                            }
                        }
                    }
                }
                "accounting-documents" -> {
                    val docs = store.accountingDocuments
                    if (docs.isEmpty()) {
                        item { Text("No accounting documents yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(docs, key = { it.id }) { rec ->
                            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenRecord(rec.id) }) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(rec.title, fontWeight = FontWeight.SemiBold)
                                    Text("${rec.entity} · ${rec.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                "payment-requests" -> {
                    val pending = store.pendingApprovals
                    if (pending.isEmpty()) {
                        item { Text("No open desk requests.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(pending, key = { it.id }) { rec ->
                            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenRecord(rec.id) }) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(rec.title, fontWeight = FontWeight.SemiBold)
                                    Text("${rec.entity} · ${rec.subtitle}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                "templates" -> {
                    items(erpTemplates) { name ->
                        Text(name, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
                "comms" -> {
                    items(erpComms) { row ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(row.first, fontWeight = FontWeight.SemiBold)
                            Text(row.second, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                "guides" -> {
                    items(erpGuides) { row ->
                        Column(Modifier.padding(bottom = 16.dp)) {
                            Text(row.first, fontWeight = FontWeight.Bold)
                            Text(row.second, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                "qna" -> {
                    items(erpQnA) { row ->
                        Column(Modifier.padding(bottom = 16.dp)) {
                            Text(row.first, fontWeight = FontWeight.Bold)
                            Text(row.second, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                "release-notes" -> {
                    item {
                        Text("$APP_NAME $APP_VERSION", fontWeight = FontWeight.Bold)
                    }
                    item {
                        Text(
                            "Every web ERP desk and feature is on the phone: departments, entity lists, approvals, trace, documents, and workspace tools. RBAC matches web IAG ERP.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                "activity-logs" -> {
                    items(store.recent.filter { store.canOpen(it.moduleId) }, key = { it.id }) { rec ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenRecord(rec.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(rec.title, fontWeight = FontWeight.SemiBold)
                                Text("${rec.entity} · ${rec.status} · ${rec.date}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                "system-health" -> {
                    item { Stat("App", "$APP_NAME $APP_VERSION") }
                    item { Stat("Role", store.user?.role ?: "—") }
                    item { Stat("Records", "${store.records.size}") }
                    item { Stat("Roles", "${store.roles.size}") }
                    item { Stat("Workspace users", "${store.workspaceUsers.size}") }
                    item { Stat("Theme", store.themeMode) }
                }
                "settings" -> {
                    item {
                        Text("Theme", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(listOf("system" to "System", "light" to "Light", "dark" to "Dark")) { option ->
                        Card(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { store.setThemeMode(option.first) },
                        ) {
                            Text(
                                option.second + if (store.themeMode == option.first) " · selected" else "",
                                modifier = Modifier.padding(16.dp),
                                fontWeight = if (store.themeMode == option.first) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                    item { Stat("Desks on this device", "${store.visibleModules.size}") }
                }
                else -> item { Text(tool?.description ?: "Unknown tool") }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private val erpTemplates = listOf(
    "Sales invoice",
    "Purchase order",
    "Expense claim",
    "General request",
    "Fuel request",
    "Leave request",
    "IPC payment certificate",
    "Payslip run",
)

private val erpComms = listOf(
    "Payslip pack — August" to "Email · HR · Sent",
    "Overdue SI-2026-188" to "SMS · Cafe Javas · Queued",
    "IPC-2026-006 submitted" to "Email · Mukwano Builders · Sent",
)

private val erpGuides = listOf(
    "Desks" to "Every sidebar tab from web IAG ERP is a department here. Open a desk to see every feature, then open a record.",
    "Clock in" to "The Clock tab is on every signed-in phone. GPS is checked against HR Sites and Blocks. Outside the fence is rejected; HR still sees punches on Attendance and Punch Log.",
    "Approvals" to "Expense claims, general requests, oral payments, leave, IPC, materials, fuel, trips, and maintenance wait on the Approvals tab when your role has a desk.",
    "SoD" to "Administrators see every app. Specialty desks (fleet, lab, CRM, …) need an explicit grant. Contractors stay on Projects and Contract Manager, plus Clock In.",
)

private val erpQnA = listOf(
    "Where is Banking?" to "Home or Departments → Treasury → Banking. Features include bank accounts, transfers, statements, and reconciliations.",
    "Who can clock in?" to "Every signed-in login, including clerk, viewer, and contractor. You do not need the HR desk.",
    "Who can approve?" to "QS, Stores, Procurement, HR, HOD, PM, Accounts, GM, CEO, Finance, and Administrators. Clerk and Viewer cannot.",
    "How do I sign in?" to "The black Sign in button uses the live web ERP password (10+ characters). To try the app here, type admin and any password of 6+ characters, then tap Continue on this device.",
)
