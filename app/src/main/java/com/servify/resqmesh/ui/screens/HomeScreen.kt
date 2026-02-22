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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servify.resqmesh.NearbyManager
import com.servify.resqmesh.ui.theme.OffGridMode
import kotlin.math.cos
import kotlin.math.sin

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

            StatusPill(isScanning, connectedPeers.size, primaryColor, nearbyManager.userName) {
                if (isScanning) nearbyManager.stopMesh() else nearbyManager.startMesh()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    RadarAnimation(primaryColor, connectedPeers.values.toList())
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
    userName: String,
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
            Column {
                Text(
                    text = if (isActive) "MESH ACTIVE — $nodeCount nodes nearby" else "MESH OFF — tap to start",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) color else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "You: $userName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RadarAnimation(color: Color, peerNames: List<String>) {
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val nodeCount = peerNames.size
    val labelStyle = TextStyle(
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerR = size.minDimension / 2

            // Static rings
            drawCircle(color = color.copy(alpha = 0.1f), radius = outerR, style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = color.copy(alpha = 0.06f), radius = outerR * 0.6f, style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = color.copy(alpha = 0.06f), radius = outerR * 0.3f, style = Stroke(width = 1.dp.toPx()))

            // Sonar sweep line
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = center,
                end = Offset(
                    (center.x + outerR * cos(sweepRad)).toFloat(),
                    (center.y + outerR * sin(sweepRad)).toFloat()
                ),
                strokeWidth = 2.dp.toPx()
            )

            // Sweep trail
            drawArc(
                color = color.copy(alpha = 0.08f),
                startAngle = sweepAngle - 60f,
                sweepAngle = 60f,
                useCenter = true,
                topLeft = Offset(center.x - outerR, center.y - outerR),
                size = androidx.compose.ui.geometry.Size(outerR * 2, outerR * 2)
            )

            // Pulse ring
            drawCircle(
                color = color.copy(alpha = pulseAlpha),
                radius = outerR * pulseRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Peer dots + name labels directly below each dot
            if (nodeCount > 0) {
                val peerRingR = outerR * 0.55f
                peerNames.forEachIndexed { index, name ->
                    val angle = Math.toRadians((index * (360.0 / nodeCount))).toDouble()
                    val dotPos = Offset(
                        (center.x + peerRingR * cos(angle)).toFloat(),
                        (center.y + peerRingR * sin(angle)).toFloat()
                    )

                    // Glow ring + solid dot
                    drawCircle(color = color.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = dotPos)
                    drawCircle(color = color, radius = 6.dp.toPx(), center = dotPos)

                    // Name label drawn just below the dot
                    val measured = textMeasurer.measure(name, labelStyle)
                    val labelX = dotPos.x - measured.size.width / 2f
                    val labelY = dotPos.y + 10.dp.toPx() // 10dp gap below dot center
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(labelX, labelY)
                    )
                }
            }
        }

        // Center "you" dot
        Surface(
            modifier = Modifier.size(20.dp),
            shape = CircleShape,
            color = color,
            shadowElevation = 8.dp
        ) {}
    }
}
