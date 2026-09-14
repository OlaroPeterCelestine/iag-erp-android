package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.AppTile
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagGroupedCard
import africa.iag.erp.android.ui.theme.IagListRow
import africa.iag.erp.android.ui.theme.QuickActionsGrid
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.WelcomeCard
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.QuickAction

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
) {
    rememberStoreTick(store)
    val firstName = store.user?.name?.split(" ")?.firstOrNull() ?: "there"
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            WelcomeCard(
                name = firstName,
                subtitle = "Pick an app to start work.",
                stats = store.welcomeStats,
            )
        }
        if (store.launcherQuickActions.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                SectionLabel("Shortcuts")
                QuickActionsGrid(
                    actions = store.launcherQuickActions,
                    pending = store.pendingApprovals.size,
                    onAction = { action: QuickAction ->
                        openQuickAction(action, onOpenDepartment, onOpenEntity, onOpenClock, onOpenAccess, onCreate, onOpenApprovals)
                    },
                )
            }
        }
        item(span = { GridItemSpan(2) }) { SectionLabel("Apps") }
        items(store.visibleSuiteApps, key = { it.id }) { app ->
            AppTile(app) { store.openApp(app.id) }
        }
        item(span = { GridItemSpan(2) }) {
            IagGroupedCard {
                IagListRow(onClick = onOpenProfile, divider = store.isAdmin) {
                    DeskRow(title = "Account", subtitle = "Profile, theme, and sign out", icon = Icons.Outlined.Person)
                }
                if (store.isAdmin) {
                    IagListRow(onClick = onOpenAccess, divider = false) {
                        DeskRow(title = "Users & roles", subtitle = "Custom roles and workspace users", icon = Icons.Outlined.Shield)
                    }
                }
            }
        }
        item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(12.dp)) }
    }
}
