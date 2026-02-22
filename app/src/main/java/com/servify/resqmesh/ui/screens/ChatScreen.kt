package com.servify.resqmesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.servify.resqmesh.NearbyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(nearbyManager: NearbyManager, onNavigateBack: () -> Unit) {
    val messages by nearbyManager.messages.collectAsState()
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Mesh Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val alignment = if (msg.sender == "Me") Alignment.End else Alignment.Start
                    val color = if (msg.isEmergency) Color.Red else if (msg.sender == "Me") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                        Text(msg.sender, style = MaterialTheme.typography.labelSmall)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = color,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(msg.content, modifier = Modifier.padding(8.dp), color = if (msg.isEmergency) Color.White else Color.Unspecified)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (text.isNotBlank()) {
                        nearbyManager.sendMessage(text)
                        text = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
