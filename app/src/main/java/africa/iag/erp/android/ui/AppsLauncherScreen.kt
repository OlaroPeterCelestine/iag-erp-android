package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.AppTile
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagEmptyHint
import africa.iag.erp.android.ui.theme.IagGroupedCard
import africa.iag.erp.android.ui.theme.IagListRow
import africa.iag.erp.android.ui.theme.IagSearchField
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.TodayActionCard
import africa.iag.erp.android.ui.theme.WelcomeCard
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.QuickAction
import africa.iag.erp.core.QuickActionKind
import africa.iag.erp.core.SearchKind

@Composable
fun AppsLauncherScreen(
    store: ErpStore,
    onOpenClock: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenApprovals: () -> Unit = {},
    onCreate: (String, String) -> Unit = { _, _ -> },
    onOpenEntity: (String, String) -> Unit = { _, _ -> },
    onOpenDepartment: (String) -> Unit = {},
    onOpenRecord: (String) -> Unit = {},
    onOpenTool: (String) -> Unit = {},
) {
    rememberStoreTick(store)
    var query by remember { mutableStateOf("") }
    val firstName = store.user?.name?.split(" ")?.firstOrNull() ?: "there"
    val todos = if (store.canApprove) store.pendingApprovals.take(3) else emptyList()
    val recents = store.recent.filter { store.canOpen(it.moduleId) }.take(3)
    val today = store.launcherQuickActions.filter { it.appId == null }
    val searching = query.trim().isNotEmpty()
    val hits = store.searchHits(query)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        if (!searching) {
            item {
                WelcomeCard(
                    name = firstName,
                    subtitle = store.user?.role ?: "Inspire Africa Group",
                    stats = store.welcomeStats,
                )
            }
        }
        item { IagSearchField(query, { query = it }, "Search anything") }
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
            if (today.isNotEmpty()) {
                item {
                    SectionLabel("Do now")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        today.forEach { action: QuickAction ->
                            Box(Modifier.weight(1f)) {
                                TodayActionCard(
                                    action = action,
                                    badge = if (action.kind == QuickActionKind.APPROVALS) store.pendingApprovals.size else 0,
                                    onClick = {
                                        openQuickAction(action, onOpenDepartment, onOpenEntity, onOpenClock, onOpenAccess, onCreate, onOpenApprovals)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item { SectionLabel("Apps") }
            store.visibleSuiteApps.chunked(3).forEach { row ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { app ->
                            Box(Modifier.weight(1f)) {
                                AppTile(app) { store.openApp(app.id) }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
                                    subtitle = rec.entity,
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
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
