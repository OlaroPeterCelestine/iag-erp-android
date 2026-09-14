package africa.iag.erp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import africa.iag.erp.core.ErpStore

val IagSlate = Color(0xFF0F172A)
val IagEmerald = Color(0xFF059669)
val IagSurface = Color(0xFFF8FAFC)
val IagOrange = Color(0xFFF97316)

private val LightColors = lightColorScheme(
    primary = IagSlate,
    onPrimary = Color.White,
    secondary = IagEmerald,
    onSecondary = Color.White,
    background = IagSurface,
    surface = Color.White,
    onBackground = IagSlate,
    onSurface = IagSlate,
    error = Color(0xFFB91C1C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = IagSlate,
    secondary = IagEmerald,
    onSecondary = Color.White,
    background = IagSlate,
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    error = Color(0xFFB91C1C),
)

@Composable
fun rememberStoreTick(store: ErpStore): Int {
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(store) {
        val listener = { tick += 1 }
        store.addListener(listener)
        onDispose { store.removeListener(listener) }
    }
    return tick
}

@Composable
fun ErpAndroidTheme(store: ErpStore, content: @Composable () -> Unit) {
    rememberStoreTick(store)
    val dark = when (store.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}

fun statusColor(status: String): Color {
    val s = status.lowercase()
    if (listOf("paid", "approved", "active", "released", "closed", "verified", "present", "posted", "cleared").any { s.contains(it) }) {
        return IagEmerald
    }
    if (listOf("overdue", "reject", "void", "cancel", "outside").any { s.contains(it) }) {
        return Color(0xFFB91C1C)
    }
    if (listOf("pending", "open", "draft", "held", "flagged", "submitted").any { s.contains(it) }) {
        return IagOrange
    }
    return Color(0xFF64748B)
}
