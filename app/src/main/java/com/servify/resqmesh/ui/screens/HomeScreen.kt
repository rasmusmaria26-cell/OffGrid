package com.servify.resqmesh.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servify.resqmesh.NearbyManager
import com.servify.resqmesh.ui.theme.OffGridMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nearbyManager: NearbyManager,
    onNavigateToChat: () -> Unit,
    onNavigateToEmergency: () -> Unit
) {
    val connectedPeers by nearbyManager.connectedPeers.collectAsState()
    val isScanning by nearbyManager.isScanning.collectAsState()
    val mode by nearbyManager.mode.collectAsState()

    val primaryColor = if (mode == OffGridMode.EMERGENCY) Color(0xFFFF4500) else Color(0xFF00B4FF)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "OFFGRID",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0F0F0F)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            StatusPill(isScanning, connectedPeers.size, primaryColor) {
                if (isScanning) nearbyManager.stopMesh() else nearbyManager.startMesh()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    RadarAnimation(primaryColor, connectedPeers.size)
                } else {
                    Text(
                        "Tap status to start mesh",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    isActive: Boolean,
    nodeCount: Int,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
        color = Color(0xFF1A1A1A)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) color else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "MESH ACTIVE — $nodeCount nodes nearby" else "MESH OFF — tap to start",
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) color else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RadarAnimation(color: Color, nodeCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val radius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            // Static outer ring
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = size.minDimension / 2,
                style = Stroke(width = 1.dp.toPx())
            )
            // Pulsing ring
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = (size.minDimension / 2) * radius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center core dot
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = color,
            shadowElevation = 8.dp
        ) {}
    }
}
