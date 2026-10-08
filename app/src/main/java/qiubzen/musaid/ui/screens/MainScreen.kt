package qiubzen.musaid.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import qiubzen.musaid.viewmodel.GoalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(startDestination: String = "routines") {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showSettings by rememberSaveable { mutableStateOf(false) }
    val isDetailScreen = currentRoute?.startsWith("target_detail") == true

    Scaffold(
        topBar = {
            if (!isDetailScreen) {
                TopAppBar(
                    title = { Text(getTopBarTitle(currentRoute)) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isDetailScreen) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Schedule, contentDescription = "Routines") },
                        label = { Text("রুটিন") },
                        selected = currentRoute == "routines",
                        onClick = {
                            navController.navigate("routines") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Flag, contentDescription = "Target") },
                        label = { Text("টার্গেট") },
                        selected = currentRoute == "target" || currentRoute?.startsWith("target_detail") == true,
                        onClick = {
                            navController.navigate("target") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Checklist, contentDescription = "Checklist") },
                        label = { Text("চেকলিস্ট") },
                        selected = currentRoute == "checklist",
                        onClick = {
                            navController.navigate("checklist") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Assignment, contentDescription = "Report") },
                        label = { Text("রিপোর্ট") },
                        selected = currentRoute == "report",
                        onClick = {
                            navController.navigate("report") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Analytics, contentDescription = "Summary") },
                        label = { Text("সারাংশ") },
                        selected = currentRoute == "summary",
                        onClick = {
                            navController.navigate("summary") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("routines") { RoutinesScreen() }
            composable("target") {
                val goalViewModel: GoalViewModel = viewModel()
                GoalsScreen(
                    viewModel = goalViewModel,
                    onOpenGoalDetail = { goalId ->
                        navController.navigate("target_detail/$goalId")
                    }
                )
            }
            composable("target_detail/{goalId}") { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId")?.toIntOrNull() ?: 0
                val goalViewModel: GoalViewModel = viewModel()
                GoalDetailScreen(
                    goalId = goalId,
                    viewModel = goalViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("checklist") { ChecklistScreen() }
            composable("report") { ReportScreen() }
            composable("summary") { SummaryScreen() }
        }

        if (showSettings) {
            SettingsDialog(onDismiss = { showSettings = false })
        }
    }
}

fun getTopBarTitle(route: String?): String {
    return when {
        route == "routines" -> "ডেইলি রুটিন"
        route == "target" || route?.startsWith("target_detail") == true -> "টার্গেট ও লক্ষ্য"
        route == "checklist" -> "অভ্যাস ও কাজ"
        route == "report" -> "দৈনিক রিপোর্ট"
        route == "summary" -> "মাসিক সারাংশ"
        else -> "Musaid"
    }
}
