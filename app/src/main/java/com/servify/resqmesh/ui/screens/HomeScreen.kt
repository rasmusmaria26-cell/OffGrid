package com.servify.resqmesh.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.servify.resqmesh.NearbyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nearbyManager: NearbyManager,
    onNavigateToChat: () -> Unit,
    onNavigateToEmergency: () -> Unit
) {
    val connectedPeers by nearbyManager.connectedPeers.collectAsState()
    val isScanning by nearbyManager.isScanning.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("OffGrid") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToEmergency,
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Emergency")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { nearbyManager.startMesh() },
                    modifier = Modifier.weight(1f),
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isScanning) "Mesh Active" else "Start Mesh")
                }
                Button(
                    onClick = { nearbyManager.stopMesh() },
                    modifier = Modifier.weight(1f),
                    enabled = isScanning
                ) {
                    Text("Stop Mesh")
                }
            }

            if (isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Scanning and Advertising...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Nearby Nodes (${connectedPeers.size})", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(connectedPeers.toList()) { (id, name) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = onNavigateToChat
                    ) {
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text("ID: $id") }
                        )
                    }
                }
            }
        }
    }
}
