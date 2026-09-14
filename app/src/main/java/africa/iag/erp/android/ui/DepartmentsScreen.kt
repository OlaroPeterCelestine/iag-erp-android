package africa.iag.erp.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.departmentGroups
import africa.iag.erp.core.featureSummary

@Composable
fun DepartmentsScreen(store: ErpStore, onOpenDepartment: (String) -> Unit) {
    rememberStoreTick(store)
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val grouped = departmentGroups.map { group ->
        group to store.visibleModules.filter { module ->
            module.group == group && (
                q.isEmpty() ||
                    module.label.lowercase().contains(q) ||
                    module.entities.any { it.lowercase().contains(q) }
                )
        }
    }.filter { it.second.isNotEmpty() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Find a desk or feature") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
            )
        }
        grouped.forEach { (group, modules) ->
            item { Text(group, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) }
            items(modules, key = { it.id }) { module ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                        store.setActiveDepartment(module.id)
                        onOpenDepartment(module.id)
                    },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(module.label, fontWeight = FontWeight.SemiBold)
                        Text(featureSummary(module), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
