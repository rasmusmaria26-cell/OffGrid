package com.servify.resqmesh.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servify.resqmesh.NearbyManager
import com.servify.resqmesh.ui.theme.OffGridMode
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    nearbyManager: NearbyManager
) {
    val connectedPeers by nearbyManager.connectedPeers.collectAsState()
    val mode by nearbyManager.mode.collectAsState()
    val primaryColor = if (mode == OffGridMode.EMERGENCY) Color(0xFFFF4500) else Color(0xFF00B4FF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Grid", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F0F),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Background Grid Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 50.dp.toPx()
                for (x in 0..(size.width / step).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(x * step, 0f),
                        end = Offset(x * step, size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..(size.height / step).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, y * step),
                        end = Offset(size.width, y * step),
                        strokeWidth = 1f
                    )
                }
            }

            // Connection Visualizer
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                NodeNetwork(connectedPeers, primaryColor, nearbyManager.userName)
            }
            
            // Legend / Info
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A).copy(alpha = 0.8f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("NETWORK STATUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Nodes: ${connectedPeers.size + 1}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Topology: P2P Mesh", style = MaterialTheme.typography.bodySmall, color = primaryColor)
                }
            }
        }
    }
}

@Composable
fun NodeNetwork(peers: Map<String, String>, color: Color, selfName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "nodeNetwork")
    val lineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineAlpha"
    )

    Canvas(modifier = Modifier.size(400.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = 120.dp.toPx()
        
        // Draw Self Node
        drawCircle(
            color = color,
            radius = 12.dp.toPx(),
            center = center
        )
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = 20.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Connections and Peer Nodes
        peers.values.toList().forEachIndexed { index, name ->
            val angle = (index * (360f / peers.size)).toDouble() * (Math.PI / 180)
            val peerPos = Offset(
                (center.x + radius * cos(angle)).toFloat(),
                (center.y + radius * sin(angle)).toFloat()
            )

            // Connection Line
            drawLine(
                color = color.copy(alpha = lineAlpha),
                start = center,
                end = peerPos,
                strokeWidth = 2.dp.toPx()
            )

            // Animated Pulse along line (Relay feel)
            // ... skipped for brevity but implied by lineAlpha ...

            // Peer Dot
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = peerPos
            )
            
            // Peer Label (would ideally be a native Text but for Canvas we'll just draw the dots)
        }
    }
}
