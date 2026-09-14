package africa.iag.erp.android.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
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
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagCard
import africa.iag.erp.android.ui.theme.WelcomeCard
import africa.iag.erp.android.ui.theme.IagOrange
import africa.iag.erp.android.ui.theme.KpiChip
import africa.iag.erp.android.ui.theme.QuickActionsGrid
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.QuickAction
import africa.iag.erp.core.QuickActionKind
import africa.iag.erp.core.SearchHit
import africa.iag.erp.core.SearchKind
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
    onCreate: (String, String) -> Unit,
    onOpenApprovals: () -> Unit,
) {
    rememberStoreTick(store)
    var query by remember { mutableStateOf("") }
    val hits = store.searchHits(query)
    val pending = if (store.canApprove) store.appPendingApprovals else emptyList()
    val firstName = store.user?.name?.split(" ")?.firstOrNull() ?: "there"
    val searching = query.trim().length >= 2

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            WelcomeCard(
                name = firstName,
                subtitle = "${store.user?.role ?: "Inspire Africa Group"} · ${store.activeSuiteApp?.label ?: "IAG ERP"}",
                stats = store.welcomeStats,
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search desks, features, records") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }
        if (searching) {
            item { SectionLabel("Search") }
            if (hits.isEmpty()) {
                item { Text("No matches in desks, features, or records.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(hits, key = { it.id }) { hit ->
                    SearchHitCard(hit, onOpenDepartment, onOpenEntity, onOpenRecord, onOpenTool)
                }
            }
        } else {
            if (store.homeQuickActions.isNotEmpty()) {
                item {
                    SectionLabel("Quick actions")
                    QuickActionsGrid(
                        actions = store.homeQuickActions,
                        pending = store.appPendingApprovals.size,
                        onAction = { openQuickAction(it, onOpenDepartment, onOpenEntity, onOpenClock, onOpenAccess, onCreate, onOpenApprovals) },
                    )
                }
            }
            item {
                SectionLabel("Snapshot")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    store.kpis.forEach { kpi ->
                        KpiChip(kpi.label, kpi.value, kpi.hint)
                    }
                }
            }
            if (store.canApprove) {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        SectionLabel("To do")
                        Spacer(Modifier.weight(1f))
                        Text("${pending.size}", color = IagOrange, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                if (pending.isEmpty()) {
                    item { Text("Nothing waiting for your desk.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(pending.take(4)) { rec ->
                        IagCard(onClick = { onOpenRecord(rec.id) }) {
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
            item { SectionLabel("Recent") }
            items(store.recent.filter { store.canOpen(it.moduleId) }.take(6)) { rec ->
                IagCard(onClick = { onOpenRecord(rec.id) }) {
                    DeskRow(
                        title = rec.title,
                        subtitle = rec.entity,
                        icon = Icons.Outlined.Description,
                        status = rec.status,
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

fun openQuickAction(
    action: QuickAction,
    onOpenDepartment: (String) -> Unit,
    onOpenEntity: (String, String) -> Unit,
    onOpenClock: () -> Unit,
    onOpenAccess: () -> Unit,
    onCreate: (String, String) -> Unit,
    onOpenApprovals: () -> Unit,
) {
    when (action.kind) {
        QuickActionKind.CLOCK -> onOpenClock()
        QuickActionKind.APPROVALS -> onOpenApprovals()
        QuickActionKind.ACCESS -> onOpenAccess()
        QuickActionKind.CREATE -> {
            val entity = action.entity
            if (entity != null) onCreate(action.moduleId, entity) else onOpenDepartment(action.moduleId)
        }
        QuickActionKind.LIST -> {
            val entity = action.entity
            if (entity != null) onOpenEntity(action.moduleId, entity) else onOpenDepartment(action.moduleId)
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
    IagCard(
        onClick = {
            when (hit.kind) {
                SearchKind.MODULE -> onOpenDepartment(hit.moduleId)
                SearchKind.ENTITY -> hit.entity?.let { onOpenEntity(hit.moduleId, it) }
                SearchKind.RECORD -> hit.recordId?.let(onOpenRecord)
                SearchKind.TOOL -> onOpenTool(hit.moduleId)
            }
        },
    ) {
        DeskRow(
            title = hit.title,
            subtitle = when (hit.kind) {
                SearchKind.MODULE -> "Desk · ${hit.subtitle}"
                SearchKind.ENTITY -> "Feature · ${hit.subtitle}"
                SearchKind.TOOL -> "Workspace · ${hit.subtitle}"
                SearchKind.RECORD -> hit.subtitle
            },
            icon = Icons.Outlined.Search,
        )
    }
}
