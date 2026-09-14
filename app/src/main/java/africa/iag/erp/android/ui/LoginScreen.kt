package africa.iag.erp.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.iag.erp.android.ui.theme.IagBrandLogo
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.APP_NAME
import africa.iag.erp.core.ErpStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Canvas = Color.White
private val Ink = Color(0xFF18181B)
private val Muted = Color(0xFF71717A)
private val Field = Color(0xFFF4F4F5)
private val Line = Color(0xFFE4E4E7)
private val ErrorFill = Color(0xFFFFF1F2)
private val ErrorText = Color(0xFFB91C1C)

@Composable
fun LoginScreen(store: ErpStore, onSignedIn: () -> Unit) {
    rememberStoreTick(store)
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }

    if (showReset) {
        ResetPasswordScreen(store = store, username = username, onBack = { showReset = false })
        return
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
        focusedContainerColor = Field,
        unfocusedContainerColor = Field,
        focusedBorderColor = Ink,
        unfocusedBorderColor = Line,
        focusedLabelColor = Ink,
        unfocusedLabelColor = Muted,
        cursorColor = Ink,
        focusedTrailingIconColor = Muted,
        unfocusedTrailingIconColor = Muted,
    )

    fun signInRemote() {
        if (busy) return
        if (username.trim().isEmpty() || password.isEmpty()) {
            error = "Enter your username and password."
            return
        }
        busy = true
        error = null
        focusManager.clearFocus()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                store.loginAsync(username, password)
            }
            busy = false
            if (result != null) error = result else onSignedIn()
        }
    }

    val canSubmit = !busy

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        IagBrandLogo(height = 56.dp, mono = true, modifier = Modifier.fillMaxWidth(0.72f))
        Spacer(Modifier.height(16.dp))
        Text(APP_NAME, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(28.dp))
        Text("Sign in", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Enter your username and password to continue.",
            color = Muted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                placeholder = { Text("Enter username") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showReset = true }) {
                    Text("Forgot password?", color = Ink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Enter password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = Muted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { if (canSubmit) signInRemote() }),
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus),
            )
            if (error != null) {
                Text(
                    error!!,
                    color = ErrorText,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .background(ErrorFill, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { signInRemote() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor = Color.White,
                    disabledContainerColor = Ink.copy(alpha = 0.35f),
                    disabledContentColor = Color.White,
                ),
            ) {
                if (busy) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sign in", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            TextButton(
                onClick = {
                    val result = store.login(username, password)
                    if (result != null) error = result else onSignedIn()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue on this device", color = Muted, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            "© Inspire Africa Group",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 28.dp),
        )
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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
        focusedContainerColor = Field,
        unfocusedContainerColor = Field,
        focusedBorderColor = Ink,
        unfocusedBorderColor = Line,
        focusedLabelColor = Ink,
        unfocusedLabelColor = Muted,
        cursorColor = Ink,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Reset password", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, shape = RoundedCornerShape(12.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("New password on this device") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error!!, color = ErrorText, modifier = Modifier.padding(top = 12.dp))
        if (notice != null) Text(notice!!, color = Muted, modifier = Modifier.padding(top = 8.dp))
        if (done) Text("Password updated. Sign in with the new password.", color = Muted)
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Field, contentColor = Ink),
        ) { Text("Save on this device") }
        TextButton(onClick = onBack) { Text("Back to sign in", color = Muted) }
    }
}
