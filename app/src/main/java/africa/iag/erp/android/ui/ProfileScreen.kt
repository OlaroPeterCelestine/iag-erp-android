package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.APP_VERSION
import africa.iag.erp.core.ErpStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(store: ErpStore, onBack: () -> Unit, onSignedOut: () -> Unit) {
    rememberStoreTick(store)
    val user = store.user
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var title by remember { mutableStateOf(user?.title ?: "") }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(user?.role ?: "", fontWeight = FontWeight.Bold)
            Text("ERP Android $APP_VERSION", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            if (message != null) Text(message!!, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { message = store.updateProfile(name, email, phone, title) ?: "Saved." },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save profile") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    store.setThemeMode(if (store.themeMode == "dark") "light" else "dark")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (store.themeMode == "dark") "Use light theme" else "Use dark theme") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    store.logout()
                    onSignedOut()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out") }
        }
    }
}
