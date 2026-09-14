package africa.iag.erp.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import africa.iag.erp.android.ui.theme.rememberStoreTick
import africa.iag.erp.core.ErpStore
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun ErpApp(store: ErpStore) {
    rememberStoreTick(store)
    val nav = rememberNavController()
    val start = if (store.isSignedIn) "shell" else "login"
    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            LoginScreen(
                store = store,
                onSignedIn = {
                    nav.navigate("shell") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }
        composable("shell") {
            ShellScreen(
                store = store,
                onOpenDepartment = { nav.navigate("department/$it") },
                onOpenEntity = { module, entity ->
                    nav.navigate("entity/$module/${URLEncoder.encode(entity, "UTF-8")}")
                },
                onOpenRecord = { nav.navigate("record/$it") },
                onOpenTool = { nav.navigate("tool/$it") },
                onOpenClock = { nav.navigate("clock") },
                onOpenAccess = { nav.navigate("access") },
                onOpenProfile = { nav.navigate("profile") },
            )
        }
        composable(
            "department/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            DepartmentScreen(
                store = store,
                moduleId = id,
                onBack = { nav.popBackStack() },
                onOpenEntity = { entity ->
                    nav.navigate("entity/$id/${URLEncoder.encode(entity, "UTF-8")}")
                },
            )
        }
        composable(
            "entity/{module}/{entity}",
            arguments = listOf(
                navArgument("module") { type = NavType.StringType },
                navArgument("entity") { type = NavType.StringType },
            ),
        ) { entry ->
            val module = entry.arguments?.getString("module") ?: return@composable
            val entity = URLDecoder.decode(entry.arguments?.getString("entity") ?: "", "UTF-8")
            if (entity == "Clock In") {
                ClockInScreen(
                    store = store,
                    onBack = { nav.popBackStack() },
                    onOpenRecord = { nav.navigate("record/$it") },
                )
            } else {
                EntityListScreen(
                    store = store,
                    moduleId = module,
                    entity = entity,
                    onBack = { nav.popBackStack() },
                    onOpenRecord = { nav.navigate("record/$it") },
                    onCreate = { nav.navigate("form/$module/${URLEncoder.encode(entity, "UTF-8")}") },
                )
            }
        }
        composable(
            "record/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            RecordDetailScreen(store = store, recordId = id, onBack = { nav.popBackStack() })
        }
        composable(
            "form/{module}/{entity}",
            arguments = listOf(
                navArgument("module") { type = NavType.StringType },
                navArgument("entity") { type = NavType.StringType },
            ),
        ) { entry ->
            val module = entry.arguments?.getString("module") ?: return@composable
            val entity = URLDecoder.decode(entry.arguments?.getString("entity") ?: "", "UTF-8")
            RecordFormScreen(
                store = store,
                moduleId = module,
                entity = entity,
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            "tool/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            if (id == "clock-in") {
                ClockInScreen(
                    store = store,
                    onBack = { nav.popBackStack() },
                    onOpenRecord = { nav.navigate("record/$it") },
                )
            } else {
                WorkspaceToolScreen(
                    store = store,
                    toolId = id,
                    onBack = { nav.popBackStack() },
                    onOpenDepartment = { nav.navigate("department/$it") },
                    onOpenEntity = { module, entity ->
                        nav.navigate("entity/$module/${URLEncoder.encode(entity, "UTF-8")}")
                    },
                    onOpenRecord = { nav.navigate("record/$it") },
                    onOpenTool = { nav.navigate("tool/$it") },
                )
            }
        }
        composable("clock") {
            ClockInScreen(
                store = store,
                onBack = { nav.popBackStack() },
                onOpenRecord = { nav.navigate("record/$it") },
            )
        }
        composable("access") {
            AccessScreen(store = store, onBack = { nav.popBackStack() })
        }
        composable("profile") {
            ProfileScreen(
                store = store,
                onBack = { nav.popBackStack() },
                onSignedOut = {
                    nav.navigate("login") {
                        popUpTo("shell") { inclusive = true }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    store: ErpStore,
    onOpenDepartment: (String) -> Unit,
    onOpenEntity: (String, String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenClock: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    rememberStoreTick(store)
    var index by remember { mutableIntStateOf(0) }
    val showApprovals = store.canApprove
    val tabs = buildList {
        add("Overview")
        add("Departments")
        add("Clock")
        if (showApprovals) add("Approvals")
        add("Workspace")
    }
    val safeIndex = index.coerceAtMost(tabs.lastIndex)
    val moreIndex = tabs.lastIndex
    val clockIndex = 2
    val approvalsIndex = if (showApprovals) 3 else -1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabs[safeIndex], fontWeight = FontWeight.Bold) },
                actions = {
                    if (store.isAdmin) {
                        IconButton(onClick = onOpenAccess) {
                            Icon(Icons.Outlined.Shield, contentDescription = "Access")
                        }
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.Person, contentDescription = "Account")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = safeIndex == 0,
                    onClick = { index = 0 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = safeIndex == 1,
                    onClick = { index = 1 },
                    icon = { Icon(Icons.Outlined.Apartment, contentDescription = null) },
                    label = { Text("Departments") },
                )
                NavigationBarItem(
                    selected = safeIndex == clockIndex,
                    onClick = { index = clockIndex },
                    icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    label = { Text("Clock") },
                )
                if (showApprovals) {
                    NavigationBarItem(
                        selected = safeIndex == approvalsIndex,
                        onClick = { index = approvalsIndex },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (store.pendingApprovals.isNotEmpty()) {
                                        Badge { Text("${store.pendingApprovals.size}") }
                                    }
                                },
                            ) {
                                Icon(Icons.Outlined.AssignmentTurnedIn, contentDescription = null)
                            }
                        },
                        label = { Text("Approvals") },
                    )
                }
                NavigationBarItem(
                    selected = safeIndex == moreIndex,
                    onClick = { index = moreIndex },
                    icon = { Icon(Icons.Outlined.Apps, contentDescription = null) },
                    label = { Text("More") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                safeIndex == 0 -> HomeScreen(store, onOpenDepartment, onOpenEntity, onOpenRecord, onOpenTool, onOpenClock, onOpenAccess)
                safeIndex == 1 -> DepartmentsScreen(store, onOpenDepartment)
                safeIndex == clockIndex -> ClockInPanel(store, onOpenRecord)
                showApprovals && safeIndex == approvalsIndex -> ApprovalsScreen(store, onOpenRecord)
                else -> MoreScreen(store, onOpenTool, onOpenAccess, onOpenProfile)
            }
        }
    }
}
