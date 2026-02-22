package com.servify.resqmesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servify.resqmesh.NearbyManager
import com.servify.resqmesh.ui.theme.OffGridMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    nearbyManager: NearbyManager,
    onNavigateBack: () -> Unit
) {
    val messages by nearbyManager.messages.collectAsState()
    val connectedPeers by nearbyManager.connectedPeers.collectAsState()
    val mode by nearbyManager.mode.collectAsState()
    var text by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    // ── Peer Picker State ─────────────────────────────────────────────────────
    var selectedPeer by remember { mutableStateOf<String?>(null) } // null = broadcast
    var peerMenuExpanded by remember { mutableStateOf(false) }

    val primaryColor = if (mode == OffGridMode.EMERGENCY) Color(0xFFFF4500) else Color(0xFF00B4FF)
    val dmColor = Color(0xFF9B59B6) // Purple for DMs

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Mesh Chat", fontWeight = FontWeight.Bold)
                            if (selectedPeer != null) {
                                Text(
                                    "→ $selectedPeer  🔒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dmColor
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Peer picker button
                        Box {
                            IconButton(onClick = { peerMenuExpanded = true }) {
                                Icon(
                                    if (selectedPeer != null) Icons.Default.Lock else Icons.Default.Person,
                                    contentDescription = "Select recipient",
                                    tint = if (selectedPeer != null) dmColor else Color.Gray
                                )
                            }
                            DropdownMenu(
                                expanded = peerMenuExpanded,
                                onDismissRequest = { peerMenuExpanded = false },
                                modifier = Modifier.background(Color(0xFF1A1A1A))
                            ) {
                                // Broadcast option
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📡  Everyone (Broadcast)", color = Color.White)
                                            if (selectedPeer == null) {
                                                Spacer(Modifier.width(8.dp))
                                                Text("✓", color = primaryColor)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPeer = null
                                        peerMenuExpanded = false
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF2A2A2A))
                                // One entry per connected peer
                                connectedPeers.values.forEach { peerName ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = dmColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(peerName, color = Color.White)
                                                if (selectedPeer == peerName) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("✓", color = dmColor)
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedPeer = peerName
                                            peerMenuExpanded = false
                                        }
                                    )
                                }
                                if (connectedPeers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No peers connected", color = Color.Gray) },
                                        onClick = { peerMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F0F0F),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F0F0F),
                    contentColor = primaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = primaryColor
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Nearby") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Groups") }
                    )
                }
            }
        },
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        msg = msg,
                        isMe = msg.sender == nearbyManager.userName,
                        primaryColor = if (msg.isDirect) dmColor else primaryColor
                    )
                }
            }

            // Quick Actions Bar (Festival mode only)
            if (mode == OffGridMode.FESTIVAL && selectedPeer == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    AssistChip(
                        onClick = { nearbyManager.sendMessage("📍 At the Main Stage", false) },
                        label = { Text("Where am I?") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1A1A1A),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // DM mode indicator banner
            if (selectedPeer != null) {
                Surface(
                    color = dmColor.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, tint = dmColor, modifier = Modifier.size(14.dp))
                        Text(
                            "Encrypted DM to $selectedPeer",
                            style = MaterialTheme.typography.labelMedium,
                            color = dmColor
                        )
                    }
                }
            }

            // Input bar
            Surface(
                color = Color(0xFF1A1A1A),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp, top = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (selectedPeer != null) "Send encrypted DM..." else "Type an offline message...",
                                color = Color.Gray
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    val sendColor = if (selectedPeer != null) dmColor else primaryColor
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                val peer = selectedPeer
                                if (peer != null) {
                                    nearbyManager.sendDirectMessage(text, peer)
                                } else {
                                    nearbyManager.sendMessage(text)
                                }
                                text = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = sendColor)
                    ) {
                        Icon(
                            if (selectedPeer != null) Icons.Default.Lock else Icons.Default.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
}

@Composable
fun ChatBubble(msg: com.servify.resqmesh.ChatMessage, isMe: Boolean, primaryColor: Color) {
    val bubbleColor = if (isMe) primaryColor else Color(0xFF2A2A2A)
    val contentColor = if (isMe) Color.Black else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = msg.sender,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            )
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = contentColor
            )
        }

        // Meta row: DM label + hop count
        Row(
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (msg.isDirect) {
                Text(
                    text = "🔒 encrypted",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = primaryColor.copy(alpha = 0.8f)
                )
            }
            Text(
                text = if (msg.hops == 0) "direct" else "via ${msg.hops} nodes",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.Gray.copy(alpha = 0.6f)
            )
        }
    }
}
