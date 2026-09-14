package africa.iag.erp.android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.North
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.RequestPage
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.South
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.iag.erp.core.QuickAction
import africa.iag.erp.core.QuickActionKind
import africa.iag.erp.core.SuiteApp
import africa.iag.erp.core.WelcomeStat

val IagCardShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun iagTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
)

@Composable
fun IagMonogram(size: Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(IagOrange),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "IAG",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.28f).sp,
        )
    }
}

@Composable
fun IconWell(icon: ImageVector, color: Color, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun StatusChip(text: String) {
    val color = statusColor(text)
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun KpiChip(label: String, value: String, hint: String) {
    Card(
        modifier = Modifier.width(148.dp),
        shape = IagCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

@Composable
fun GreetingBlock(name: String, subtitle: String) {
    Column {
        Text(
            "${greetingLabel()}, $name",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun WelcomeCard(name: String, subtitle: String, stats: List<WelcomeStat>) {
    val onOrange = Color.White
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(18.dp),
    ) {
        Text(
            "Welcome back",
            color = onOrange.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${greetingLabel()}, $name",
            color = onOrange,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = onOrange.copy(alpha = 0.85f), fontSize = 14.sp)
        if (stats.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                stats.forEach { stat ->
                    Column(Modifier.weight(1f)) {
                        Text(
                            stat.value,
                            color = onOrange,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stat.label,
                            color = onOrange.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeskRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color = IagOrange,
    status: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconWell(icon, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (status != null) {
            Spacer(Modifier.width(8.dp))
            StatusChip(status)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IagCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = IagCardShape,
            colors = colors,
            elevation = elevation,
            border = border,
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = IagCardShape,
            colors = colors,
            elevation = elevation,
            border = border,
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun AppTile(app: SuiteApp, onClick: () -> Unit) {
    val color = app.color.toComposeColor()
    IagCard(onClick = onClick) {
        IconWell(suiteAppIcon(app.id), color)
        Spacer(Modifier.height(14.dp))
        Text(app.label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            app.description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(48.dp),
        )
    }
}

fun suiteAppIcon(id: String): ImageVector = when (id) {
    "finance" -> Icons.Outlined.AccountBalance
    "procurement" -> Icons.Outlined.ShoppingCart
    "production" -> Icons.Outlined.PrecisionManufacturing
    "security" -> Icons.Outlined.Security
    "hr" -> Icons.Outlined.Badge
    "projects" -> Icons.Outlined.WorkOutline
    "fleet", "logistics" -> Icons.Outlined.LocalShipping
    "sales" -> Icons.Outlined.Storefront
    "quality" -> Icons.Outlined.Science
    "requests" -> Icons.AutoMirrored.Outlined.Assignment
    "records" -> Icons.Outlined.Folder
    else -> Icons.Outlined.Apps
}

fun quickActionIcon(id: String): ImageVector = when (id) {
    "clock" -> Icons.Outlined.Schedule
    "approvals" -> Icons.Outlined.AssignmentTurnedIn
    "access" -> Icons.Outlined.Shield
    "receipt" -> Icons.Outlined.South
    "payment" -> Icons.Outlined.North
    "claim" -> Icons.AutoMirrored.Outlined.ReceiptLong
    "journal" -> Icons.AutoMirrored.Outlined.MenuBook
    "reports" -> Icons.Outlined.BarChart
    "transfer" -> Icons.Outlined.SwapHoriz
    "po" -> Icons.Outlined.ShoppingCart
    "grn", "shipment" -> Icons.Outlined.Inventory2
    "supplier" -> Icons.Outlined.Groups
    "item" -> Icons.Outlined.Inventory
    "prod-order" -> Icons.Outlined.PrecisionManufacturing
    "batch" -> Icons.Outlined.Layers
    "roast" -> Icons.Outlined.LocalFireDepartment
    "downtime" -> Icons.Outlined.PauseCircle
    "gate" -> Icons.Outlined.LockOpen
    "visitor", "customer" -> Icons.Outlined.PersonAdd
    "incident" -> Icons.Outlined.Report
    "leave" -> Icons.Outlined.Event
    "employee" -> Icons.Outlined.Badge
    "payroll" -> Icons.Outlined.Payments
    "project" -> Icons.Outlined.WorkOutline
    "ipc" -> Icons.Outlined.RequestQuote
    "material" -> Icons.Outlined.Hardware
    "contractor" -> Icons.Outlined.Engineering
    "fuel" -> Icons.Outlined.LocalGasStation
    "trip" -> Icons.Outlined.Map
    "maintenance" -> Icons.Outlined.Build
    "vehicle" -> Icons.Outlined.LocalShipping
    "invoice" -> Icons.Outlined.Description
    "quote" -> Icons.Outlined.RequestPage
    "lead" -> Icons.Outlined.StarOutline
    "dispatch" -> Icons.AutoMirrored.Outlined.ListAlt
    "delivery" -> Icons.Outlined.LocalShipping
    "lab" -> Icons.Outlined.Science
    "qa" -> Icons.Outlined.Verified
    "nc" -> Icons.Outlined.Report
    "gen-request" -> Icons.AutoMirrored.Outlined.Assignment
    "oral" -> Icons.Outlined.Mic
    "folder" -> Icons.Outlined.CreateNewFolder
    "attachment" -> Icons.Outlined.AttachFile
    else -> Icons.Outlined.Bolt
}

@Composable
fun QuickActionButton(action: QuickAction, badge: Int = 0, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(quickActionIcon(action.id), contentDescription = action.label, tint = color, modifier = Modifier.size(22.dp))
            }
            if (badge > 0) {
                Text(
                    if (badge > 9) "9+" else "$badge",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB91C1C))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            action.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun QuickActionsGrid(
    actions: List<QuickAction>,
    pending: Int = 0,
    onAction: (QuickAction) -> Unit,
) {
    IagCard {
        actions.chunked(4).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                row.forEach { action ->
                    Box(Modifier.weight(1f)) {
                        QuickActionButton(
                            action = action,
                            badge = if (action.kind == QuickActionKind.APPROVALS) pending else 0,
                            onClick = { onAction(action) },
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
