package com.servify.resqmesh.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.servify.resqmesh.NearbyManager
import com.servify.resqmesh.ui.screens.ChatScreen
import com.servify.resqmesh.ui.screens.EmergencyScreen
import com.servify.resqmesh.ui.screens.HomeScreen

@Composable
fun OffGridApp(nearbyManager: NearbyManager) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { 
            HomeScreen(nearbyManager, onNavigateToChat = { navController.navigate("chat") }, onNavigateToEmergency = { navController.navigate("emergency") })
        }
        composable("chat") { 
            ChatScreen(nearbyManager, onNavigateBack = { navController.popBackStack() }) 
        }
        composable("emergency") { 
            EmergencyScreen(nearbyManager, onNavigateBack = { navController.popBackStack() }) 
        }
    }
}
