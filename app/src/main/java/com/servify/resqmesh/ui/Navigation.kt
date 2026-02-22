package com.servify.resqmesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.servify.resqmesh.NearbyManager
import com.servify.resqmesh.ui.screens.*
import com.servify.resqmesh.ui.theme.OffGridTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Radar : Screen("home", "Radar", Icons.Default.WifiTethering)
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Grid : Screen("location", "Grid", Icons.Default.GridView)
    object SOS : Screen("emergency", "SOS", Icons.Default.Warning)
}

@Composable
fun OffGridApp(nearbyManager: NearbyManager, onNameConfirmed: (String) -> Unit = {}) {
    val navController = rememberNavController()
    val mode by nearbyManager.mode.collectAsState()
    val emergencyAlert by nearbyManager.emergencyAlert.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    OffGridTheme(mode = mode) {
        val showBottomBar = currentDestination?.route != "onboarding"

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = Color(0xFF1A1A1A),
                            tonalElevation = 0.dp
                        ) {
                            val items = listOf(Screen.Radar, Screen.Chat, Screen.Grid, Screen.SOS)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = "onboarding",
                    modifier = Modifier.padding(padding)
                ) {
                    composable("onboarding") {
                        OnboardingScreen(nearbyManager, onFinish = {
                            onNameConfirmed(nearbyManager.userName)
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        })
                    }
                    composable("home") {
                        HomeScreen(
                            nearbyManager,
                            onNavigateToChat = { navController.navigate("chat") },
                            onNavigateToEmergency = { navController.navigate("emergency") }
                        )
                    }
                    composable("chat") {
                        ChatScreen(nearbyManager, onNavigateBack = { navController.popBackStack() })
                    }
                    composable("location") {
                        GridScreen(nearbyManager)
                    }
                    composable("emergency") {
                        EmergencyScreen(nearbyManager, onNavigateBack = { navController.popBackStack() })
                    }
                }
            }

            // Full-screen emergency takeover overlay
            emergencyAlert?.let { alert ->
                EmergencyAlertOverlay(alert, onDismiss = { nearbyManager.dismissEmergency() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyAlertOverlay(alert: com.servify.resqmesh.ChatMessage, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFF4500))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            Text(
                "EMERGENCY ALERT",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "From: ${alert.sender}",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                alert.content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFFF4500)
                )
            ) {
                Text("DISMISS ALERT")
            }
        }
    }
}
