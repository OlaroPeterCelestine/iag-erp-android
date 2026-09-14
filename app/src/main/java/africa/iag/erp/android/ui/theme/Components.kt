package africa.iag.erp.android.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import africa.iag.erp.android.R
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.PointOfSale
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
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

val IagCardShape = RoundedCornerShape(22.dp)

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
            .clip(RoundedCornerShape((size.value * 0.28f).dp))
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
fun IagBrandLogo(modifier: Modifier = Modifier, height: Dp = 128.dp, mono: Boolean = false) {
    Image(
        painter = painterResource(if (mono) R.drawable.iag_logo_mono else R.drawable.iag_logo),
        contentDescription = "Inspire Africa Group",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun IconWell(icon: ImageVector, color: Color, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
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
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun SectionLabel(text: String, accessory: String? = null, onAccessory: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        if (accessory != null) {
            Text(
                accessory,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = if (onAccessory != null) Modifier.clickable(onClick = onAccessory) else Modifier,
            )
        }
    }
}

@Composable
fun IagEmptyHint(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
fun IagSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        ),
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column {
            Text(
                greetingLabel(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        if (stats.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                stats.forEach { stat ->
                    val hot = stat.id == "todo" && stat.value != "0"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (hot) IagOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stat.value,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = if (hot) IagOrange else MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            stat.label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
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
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = IagCardShape,
            colors = colors,
            elevation = elevation,
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = IagCardShape,
            colors = colors,
            elevation = elevation,
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun IagGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = IagCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(content = content)
    }
}

@Composable
fun IagListRow(
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(16.dp),
        ) { content() }
        if (divider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
fun AppTile(app: SuiteApp, subtitle: String? = null, onClick: () -> Unit) {
    val color = app.color.toComposeColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.72f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(suiteAppIcon(app.id), contentDescription = app.label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
        if (subtitle != null) {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
    }
}

fun suiteAppIcon(id: String): ImageVector = when (id) {
    "finance" -> Icons.Outlined.AccountBalance
    "procurement" -> Icons.Outlined.ShoppingCart
    "production" -> Icons.Outlined.PrecisionManufacturing
    "security" -> Icons.Outlined.Security
    "hr" -> Icons.Outlined.Badge
    "projects" -> Icons.Outlined.WorkOutline
    "contracts" -> Icons.Outlined.Handshake
    "fleet" -> Icons.Outlined.DirectionsCar
    "logistics" -> Icons.Outlined.LocalShipping
    "sales" -> Icons.Outlined.Storefront
    "crm" -> Icons.Outlined.Groups
    "pos" -> Icons.Outlined.PointOfSale
    "quality" -> Icons.Outlined.Science
    "requests" -> Icons.AutoMirrored.Outlined.Assignment
    "dms", "records" -> Icons.Outlined.FolderShared
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
    "contract" -> Icons.Outlined.Handshake
    "fuel" -> Icons.Outlined.LocalGasStation
    "trip" -> Icons.Outlined.Map
    "maintenance" -> Icons.Outlined.Build
    "vehicle" -> Icons.Outlined.DirectionsCar
    "invoice" -> Icons.Outlined.Description
    "quote" -> Icons.Outlined.RequestPage
    "lead" -> Icons.Outlined.StarOutline
    "opportunity" -> Icons.Outlined.StarOutline
    "ticket" -> Icons.Outlined.ConfirmationNumber
    "pos-sale" -> Icons.Outlined.PointOfSale
    "session" -> Icons.Outlined.LockOpen
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
    val color = action.color.toComposeColor()
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(quickActionIcon(action.id), contentDescription = action.label, tint = color, modifier = Modifier.size(20.dp))
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun TodayActionCard(action: QuickAction, badge: Int = 0, onClick: () -> Unit) {
    val color = action.color.toComposeColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(quickActionIcon(action.id), contentDescription = action.label, tint = color, modifier = Modifier.size(22.dp))
            if (badge > 0) {
                Text(
                    if (badge > 9) "9+" else "$badge",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 14.dp, y = (-10).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB91C1C))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(action.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun QuickActionRail(
    actions: List<QuickAction>,
    pending: Int = 0,
    onAction: (QuickAction) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actions.forEach { action ->
            QuickActionButton(
                action = action,
                badge = if (action.kind == QuickActionKind.APPROVALS) pending else 0,
                onClick = { onAction(action) },
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    actions: List<QuickAction>,
    pending: Int = 0,
    onAction: (QuickAction) -> Unit,
) {
    IagGroupedCard {
        actions.chunked(4).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
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
