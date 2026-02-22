package com.servify.resqmesh.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.servify.resqmesh.NearbyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(nearbyManager: NearbyManager, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Alert") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "TAP BUTTON TO BROADCAST EMERGENCY",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = Color.Red
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { nearbyManager.sendMessage("EMERGENCY SIGNAL FROM " + android.os.Build.MODEL, isEmergency = true) },
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(Icons.Default.Warning, contentDescription = "SOS", modifier = Modifier.size(80.dp), tint = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "This will send an alert to all nearby devices in the mesh network.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
