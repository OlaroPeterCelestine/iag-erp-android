package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import africa.iag.erp.android.ui.theme.IagCard
import africa.iag.erp.android.ui.theme.StatusChip
import africa.iag.erp.android.ui.theme.iagTopBarColors
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpRecord
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.formatMoney
import africa.iag.erp.core.newId
import africa.iag.erp.core.todayIsoDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(store: ErpStore, recordId: String, onBack: () -> Unit) {
    rememberStoreTick(store)
    val record = store.records.firstOrNull { it.id == recordId }
    if (record == null) {
        Text("Record not found", modifier = Modifier.padding(24.dp))
        return
    }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(record.title) },
                colors = iagTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            IagCard {
                Text(record.entity, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(record.subtitle)
                Spacer(Modifier.height(8.dp))
                StatusChip(record.status)
                Spacer(Modifier.height(6.dp))
                Text(record.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
                record.amount?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(formatMoney(it), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            record.fields.forEach { (k, v) ->
                Text("$k: $v", modifier = Modifier.padding(top = 4.dp))
            }
            if (message != null) {
                Text(message!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (store.isOpenStatus(record.status).not() && record.status.equals("Draft", true) &&
                (store.canCreate(record.moduleId, record.entity) || store.canEdit(record.moduleId, record.entity))
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                if (store.remoteSession) store.submitRecordAsync(record) else store.submitRecord(record)
                            }
                            message = result ?: "Submitted."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Submit")
                }
            }
            if (store.canApproveModule(record.moduleId) && store.isOpenStatus(record.status)) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                if (store.remoteSession) store.approveRecordAsync(record) else store.approveRecord(record)
                            }
                            message = result ?: "Approved."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Approve")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                if (store.remoteSession) store.rejectRecordAsync(record) else store.rejectRecord(record)
                            }
                            message = result ?: "Rejected."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reject")
                }
            }
            if (store.canVoid(record.moduleId)) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                if (store.remoteSession) store.voidRecordAsync(record) else store.voidRecord(record)
                            }
                            message = result ?: "Voided."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Void")
                }
            }
            if (store.canDelete(record.moduleId, record.entity)) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val err = withContext(Dispatchers.IO) {
                                if (store.remoteSession) store.deleteRecordAsync(record) else store.deleteRecord(record)
                            }
                            if (err == null) onBack() else message = err
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormScreen(store: ErpStore, moduleId: String, entity: String, onDone: () -> Unit) {
    rememberStoreTick(store)
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New $entity") },
                colors = iagTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (!store.canCreate(moduleId, entity)) {
                Text("Your role cannot create this record.")
                return@Column
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (UGX)") }, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        error = "Title is required."
                        return@Button
                    }
                    store.addRecord(
                        ErpRecord(
                            id = newId(),
                            moduleId = moduleId,
                            entity = entity,
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            status = "Draft",
                            date = todayIsoDate(),
                            amount = amount.trim().toDoubleOrNull(),
                        ),
                    )
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save draft") }
        }
    }
}
