package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagGroupedCard
import africa.iag.erp.android.ui.theme.IagListRow
import africa.iag.erp.android.ui.theme.IagSearchField
import africa.iag.erp.android.ui.theme.SectionLabel
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.android.ui.theme.toComposeColor
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.departmentGroups
import africa.iag.erp.core.featureSummary

@Composable
fun DepartmentsScreen(store: ErpStore, onOpenDepartment: (String) -> Unit) {
    rememberStoreTick(store)
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val grouped = departmentGroups.map { group ->
        group to store.appModules.filter { module ->
            module.group == group && (
                q.isEmpty() ||
                    module.label.lowercase().contains(q) ||
                    module.entities.any { it.lowercase().contains(q) }
                )
        }
    }.filter { it.second.isNotEmpty() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            IagSearchField(query, { query = it }, "Find a desk or feature")
        }
        grouped.forEach { (group, modules) ->
            item {
                SectionLabel(group)
                IagGroupedCard {
                    modules.forEachIndexed { index, module ->
                        IagListRow(
                            onClick = {
                                store.setActiveDepartment(module.id)
                                onOpenDepartment(module.id)
                            },
                            divider = index < modules.lastIndex,
                        ) {
                            DeskRow(
                                title = module.label,
                                subtitle = featureSummary(module),
                                icon = Icons.Outlined.Apartment,
                                color = module.color.toComposeColor(),
                            )
                        }
                    }
                }
            }
        }
    }
}
