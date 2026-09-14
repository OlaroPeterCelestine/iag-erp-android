package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.DeskRow
import africa.iag.erp.android.ui.theme.IagCard
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Find a desk or feature") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }
        grouped.forEach { (group, modules) ->
            item { SectionLabel(group) }
            items(modules, key = { it.id }) { module ->
                IagCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = {
                        store.setActiveDepartment(module.id)
                        onOpenDepartment(module.id)
                    },
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
