package africa.iag.erp.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.iag.erp.android.ui.theme.IagBrandLogo
import africa.iag.erp.android.ui.theme.IagOrange
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.APP_NAME
import africa.iag.erp.core.ErpStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF0A0A0D)
private val OrangeDeep = Color(0xFFEA580C)
private val MapGreen = Color(0xFF16A34A)
private val MapBlue = Color(0xFF0EA5E9)
private val MapRed = Color(0xFFEF4444)

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

    if (showReset) {
        ResetPasswordScreen(store = store, username = username, onBack = { showReset = false })
        return
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color.White.copy(alpha = 0.06f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
        focusedBorderColor = IagOrange.copy(alpha = 0.85f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
        focusedLabelColor = Color.White.copy(alpha = 0.7f),
        unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
        cursorColor = IagOrange,
    )

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        LoginAtmosphere()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            IagBrandLogo(height = 118.dp)
            Spacer(Modifier.height(22.dp))
            Text(
                "FINANCE & OPERATIONS",
                color = IagOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(APP_NAME, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Sign in to your workspace.", color = Color.White.copy(alpha = 0.62f), fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Finance  ·  Sales  ·  Projects  ·  HR",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                    .padding(22.dp),
            ) {
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
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = Color.White.copy(alpha = 0.55f),
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { showReset = true }, modifier = Modifier.align(Alignment.End)) {
                    Text("Forgot password?", color = IagOrange, fontWeight = FontWeight.SemiBold)
                }
                if (error != null) {
                    Text(error!!, color = Color(0xFFF87171), modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth())
                }
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
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                    contentPadding = PaddingValues(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(IagOrange, OrangeDeep)),
                                RoundedCornerShape(16.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sign in", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        }
                    }
                }
            }
            Text(
                "Inspire Africa Group",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp, bottom = 36.dp),
            )
        }
    }
}

@Composable
private fun LoginAtmosphere() {
    Box(Modifier.fillMaxSize()) {
        Glow(MapGreen.copy(alpha = 0.28f), 280.dp, Modifier.offset(x = (-90).dp, y = (-20).dp).align(Alignment.TopStart))
        Glow(IagOrange.copy(alpha = 0.32f), 260.dp, Modifier.offset(x = 90.dp, y = 10.dp).align(Alignment.TopEnd))
        Glow(MapBlue.copy(alpha = 0.22f), 220.dp, Modifier.offset(x = 40.dp, y = 180.dp).align(Alignment.TopCenter))
        Glow(MapRed.copy(alpha = 0.18f), 200.dp, Modifier.offset(x = (-70).dp, y = 240.dp).align(Alignment.TopStart))
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Ink.copy(alpha = 0.72f)),
                    ),
                ),
        )
    }
}

@Composable
private fun Glow(color: Color, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(
                Brush.radialGradient(listOf(color, Color.Transparent)),
                CircleShape,
            ),
    )
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
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = IagOrange.copy(alpha = 0.85f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
        focusedLabelColor = Color.White.copy(alpha = 0.7f),
        unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
        cursorColor = IagOrange,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Reset password", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
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
            colors = ButtonDefaults.buttonColors(containerColor = IagOrange, contentColor = Color.White),
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
