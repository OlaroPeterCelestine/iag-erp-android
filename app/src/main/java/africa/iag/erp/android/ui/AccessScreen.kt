package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.Crud
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.PAGE_WILDCARD_KEY
import africa.iag.erp.core.RoleDefinition
import africa.iag.erp.core.newRoleId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessScreen(store: ErpStore, onBack: () -> Unit) {
    rememberStoreTick(store)
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Access") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!store.isAdmin) {
                Text("Only administrators can manage roles and users.", modifier = Modifier.padding(16.dp))
                return@Column
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Roles") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Users") })
            }
            if (tab == 0) RolesPanel(store) else UsersPanel(store)
        }
    }
}

@Composable
private fun RolesPanel(store: ErpStore) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var view by remember { mutableStateOf(true) }
    var create by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var restrict by remember { mutableStateOf(false) }
    val grants = remember { mutableStateMapOf<String, Boolean>() }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Custom roles", fontWeight = FontWeight.Bold)
        Text("Grant CRUD and optional apps. Built-in desks stay on the same SoD allow-lists as web ERP.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        store.customRoles.forEach { role ->
            Text("• ${role.name} — ${role.description.ifBlank { "custom" }}", modifier = Modifier.padding(bottom = 4.dp))
        }
        if (store.customRoles.isEmpty()) Text("No custom roles yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Create custom role", fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        FlagRow("View", view) { view = it }
        FlagRow("Create", create) { create = it }
        FlagRow("Edit", edit) { edit = it }
        FlagRow("Delete", delete) { delete = it }
        FlagRow("Restrict to granted apps", restrict) { restrict = it }
        if (restrict) {
            store.modules.forEach { module ->
                FlagRow(module.label, grants[module.id] == true) { grants[module.id] = it }
            }
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val crud = Crud(view, create, edit, delete)
                val pages = linkedMapOf<String, Crud>()
                if (restrict) {
                    pages[PAGE_WILDCARD_KEY] = Crud.none
                    grants.filterValues { it }.keys.forEach { pages[it] = crud }
                }
                error = store.saveRole(
                    RoleDefinition(
                        id = newRoleId(),
                        name = name,
                        description = description,
                        crud = crud,
                        pagePermissions = pages,
                    ),
                )
                if (error == null) {
                    name = ""
                    description = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save role") }
    }
}

@Composable
private fun UsersPanel(store: ErpStore) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Viewer") }
    var password by remember { mutableStateOf("iagdemo") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Workspace users", fontWeight = FontWeight.Bold)
        store.workspaceUsers.forEach { user ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.SemiBold)
                    Text("${user.username} · ${user.role}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { error = store.deleteWorkspaceUser(user.username) }) { Text("Remove") }
            }
        }
        if (store.workspaceUsers.isEmpty()) Text("No custom users yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                error = store.saveWorkspaceUser(username = username, name = name, role = role, password = password)
                if (error == null) {
                    username = ""
                    name = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save user") }
    }
}

@Composable
private fun FlagRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}
