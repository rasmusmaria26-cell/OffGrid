package com.servify.resqmesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servify.resqmesh.NearbyManager
import com.servify.resqmesh.ui.theme.OffGridMode

@Composable
fun OnboardingScreen(
    nearbyManager: NearbyManager,
    onFinish: () -> Unit
) {
    var nameInput by remember { mutableStateOf(nearbyManager.userName) }
    val focusManager = LocalFocusManager.current
    val nameError = nameInput.isBlank()

    fun finish(mode: OffGridMode) {
        val finalName = nameInput.trim().ifBlank { nearbyManager.userName }
        nearbyManager.userName = finalName
        nearbyManager.setMode(mode)
        onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OffGrid",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No internet needed.\nFind your people.",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Name Field ──────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Your name on the mesh",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it.take(24) }, // cap at 24 chars
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Alex or BlueHawk", color = Color(0xFF555555)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00B4FF),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00B4FF),
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A)
                ),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    Text(
                        "${nameInput.length}/24 · This is how peers will see you",
                        color = Color(0xFF555555),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Choose your mode",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        ModeButton(
            title = "Festival / Event",
            subtitle = "Electric vibes, group chats, zero lag.",
            icon = Icons.Default.Star,
            color = Color(0xFF00B4FF),
            enabled = !nameError,
            onClick = { finish(OffGridMode.FESTIVAL) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ModeButton(
            title = "Emergency",
            subtitle = "High-priority alerts & rescue mesh.",
            icon = Icons.Default.Notifications,
            color = Color(0xFFFF4500),
            enabled = !nameError,
            onClick = { finish(OffGridMode.EMERGENCY) }
        )
    }
}

@Composable
fun ModeButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A1A1A),
            contentColor = color,
            disabledContainerColor = Color(0xFF111111),
            disabledContentColor = Color(0xFF444444)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (enabled) color else Color(0xFF444444)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) Color.White else Color(0xFF444444),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) Color.Gray else Color(0xFF333333)
                )
            }
        }
    }
}
