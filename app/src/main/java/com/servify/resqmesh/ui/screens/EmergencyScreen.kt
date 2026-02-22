package com.servify.resqmesh.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.servify.resqmesh.NearbyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    nearbyManager: NearbyManager,
    onNavigateBack: () -> Unit
) {
    val connectedPeers by nearbyManager.connectedPeers.collectAsState()
    var isSent by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Haptic helper
    fun triggerSosHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 200, 100, 500),
                        intArrayOf(0, 255, 0, 255, 0, 200),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Vibrator::class.java)
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 200, 100, 500), -1
                    )
                )
            }
        } catch (e: Exception) { /* Vibrator not available on some devices */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency SOS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F0F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F0F)
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
                text = if (connectedPeers.isEmpty())
                    "Start the mesh on the Radar screen first to alert nearby nodes."
                else
                    "This will broadcast an SOS to all ${connectedPeers.size} connected nodes instantly.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                // Outer glow rings
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(200.dp + (i * 40).dp)
                            .scale(pulseScale)
                            .border(2.dp, Color(0xFFFF4500).copy(alpha = 0.3f / (i + 1)), CircleShape)
                    )
                }

                // SOS Button
                Surface(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(if (isSent) 1f else pulseScale)
                        .clip(CircleShape)
                        .clickable(enabled = !isSent) {
                            nearbyManager.sendMessage("🚨 EMERGENCY SOS — Needs immediate help!", true)
                            isSent = true
                            triggerSosHaptic()
                        },
                    color = if (isSent) Color(0xFF8B0000) else Color(0xFFFF4500),
                    shape = CircleShape,
                    shadowElevation = 12.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "SOS",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                            if (isSent) {
                                Text(
                                    "SENT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                Text(
                                    "SOS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (isSent) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🚨  SOS BROADCAST SENT",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFF4500),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${connectedPeers.size} node${if (connectedPeers.size != 1) "s" else ""} alerted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            "Message is relaying across the mesh",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
