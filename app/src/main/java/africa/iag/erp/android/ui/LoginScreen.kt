package africa.iag.erp.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.iag.erp.android.ui.theme.IagBrandLogo
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.APP_NAME
import africa.iag.erp.core.ErpStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(store: ErpStore, onSignedIn: () -> Unit) {
    rememberStoreTick(store)
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showReset) {
        ResetPasswordScreen(store = store, username = username, onBack = { showReset = false })
        return
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color.White.copy(alpha = 0.08f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
        focusedBorderColor = Color.White.copy(alpha = 0.28f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
        focusedLabelColor = Color.White.copy(alpha = 0.7f),
        unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
        cursorColor = Color.White,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        IagBrandLogo(height = 84.dp, mono = true)
        Spacer(Modifier.height(28.dp))
        Text(APP_NAME, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Sign in to continue", color = Color.White.copy(alpha = 0.55f))
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { showReset = true }, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?", color = Color.White.copy(alpha = 0.7f))
        }
        if (error != null) {
            Text(error!!, color = Color(0xFFF87171), modifier = Modifier.padding(top = 8.dp).fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        store.loginAsync(username, password)
                    }
                    busy = false
                    if (result != null) error = result else onSignedIn()
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            if (busy) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign in", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(store: ErpStore, username: String, onBack: () -> Unit) {
    var user by remember { mutableStateOf(username) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Reset password", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White.copy(alpha = 0.28f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
            focusedLabelColor = Color.White.copy(alpha = 0.7f),
            unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
            cursorColor = Color.White,
        )
        OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("New password on this device") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error!!, color = Color(0xFFF87171), modifier = Modifier.padding(top = 12.dp))
        if (notice != null) Text(notice!!, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 8.dp))
        if (done) Text("Password updated. Sign in with the new password.", color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                error = null
                notice = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { store.requestFrontendPasswordReset(user) }
                    busy = false
                    result.fold(
                        onSuccess = { message -> notice = message },
                        onFailure = { error = it.message },
                    )
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) { Text(if (busy) "Sending…" else "Email reset code") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val result = store.resetPassword(user, password, confirm)
                if (result != null) {
                    error = result
                    done = false
                } else {
                    error = null
                    done = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
        ) { Text("Save on this device") }
        TextButton(onClick = onBack) { Text("Back to sign in", color = Color.White.copy(alpha = 0.7f)) }
    }
}
