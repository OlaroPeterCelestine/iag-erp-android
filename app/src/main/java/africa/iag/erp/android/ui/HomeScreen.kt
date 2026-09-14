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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagEmptyHint
import africa.iag.erp.android.ui.theme.IagGroupedCard
import africa.iag.erp.android.ui.theme.IagListRow
import africa.iag.erp.android.ui.theme.IagSearchField
import africa.iag.erp.android.ui.theme.KpiChip
import africa.iag.erp.android.ui.theme.QuickActionRail
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.WelcomeCard
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.APP_NAME
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.QuickAction
import africa.iag.erp.core.QuickActionKind
import africa.iag.erp.core.SearchHit
import africa.iag.erp.core.SearchKind

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
    val todos = pending.take(3)
    val recents = store.recent.filter { store.canOpen(it.moduleId) }.take(3)
    val firstName = store.user?.name?.split(" ")?.firstOrNull() ?: "there"
    val searching = query.trim().isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        if (!searching) {
            item {
                WelcomeCard(
                    name = firstName,
                    subtitle = store.activeSuiteApp?.label ?: store.user?.role ?: APP_NAME,
                    stats = store.welcomeStats,
                )
            }
        }
        item {
            IagSearchField(query, { query = it }, "Search desks and records")
        }
        if (searching) {
            item { SectionLabel("Search") }
            item {
                IagGroupedCard {
                    if (hits.isEmpty()) {
                        IagEmptyHint("No matches in desks, features, or records.")
                    } else {
                        hits.forEachIndexed { index, hit ->
                            IagListRow(
                                onClick = { openSearchHit(hit, onOpenDepartment, onOpenEntity, onOpenRecord, onOpenTool) },
                                divider = index < hits.lastIndex,
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
                    }
                }
            }
        } else {
            if (store.homeQuickActions.isNotEmpty()) {
                item {
                    SectionLabel("Do now")
                    QuickActionRail(
                        actions = store.homeQuickActions,
                        pending = store.appPendingApprovals.size,
                        onAction = { openQuickAction(it, onOpenDepartment, onOpenEntity, onOpenClock, onOpenAccess, onCreate, onOpenApprovals) },
                    )
                }
            }
            if (store.kpis.isNotEmpty()) {
                item {
                    SectionLabel("Snapshot")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        store.kpis.forEach { kpi ->
                            KpiChip(kpi.label, kpi.value, kpi.hint)
                        }
                    }
                }
            }
            if (store.canApprove && todos.isNotEmpty()) {
                item {
                    SectionLabel("Waiting for you", accessory = "See all", onAccessory = onOpenApprovals)
                    IagGroupedCard {
                        todos.forEachIndexed { index, rec ->
                            IagListRow(onClick = { onOpenRecord(rec.id) }, divider = index < todos.lastIndex) {
                                DeskRow(
                                    title = rec.title,
                                    subtitle = "${rec.entity} · ${rec.subtitle}",
                                    icon = Icons.Outlined.AssignmentTurnedIn,
                                    status = rec.status,
                                )
                            }
                        }
                    }
                }
            }
            if (recents.isNotEmpty()) {
                item {
                    SectionLabel("Jump back")
                    IagGroupedCard {
                        recents.forEachIndexed { index, rec ->
                            IagListRow(onClick = { onOpenRecord(rec.id) }, divider = index < recents.lastIndex) {
                                DeskRow(
                                    title = rec.title,
                                    subtitle = rec.entity,
                                    icon = Icons.Outlined.Description,
                                    status = rec.status,
                                )
                            }
                        }
                    }
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

fun openSearchHit(
    hit: SearchHit,
    onOpenDepartment: (String) -> Unit,
    onOpenEntity: (String, String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTool: (String) -> Unit,
) {
    when (hit.kind) {
        SearchKind.MODULE -> onOpenDepartment(hit.moduleId)
        SearchKind.ENTITY -> hit.entity?.let { onOpenEntity(hit.moduleId, it) }
        SearchKind.RECORD -> hit.recordId?.let(onOpenRecord)
        SearchKind.TOOL -> onOpenTool(hit.moduleId)
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
    IagGroupedCard {
        IagListRow(
            onClick = { openSearchHit(hit, onOpenDepartment, onOpenEntity, onOpenRecord, onOpenTool) },
            divider = false,
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
}
