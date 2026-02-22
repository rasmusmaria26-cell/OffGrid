package com.servify.resqmesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val mode by nearbyManager.mode.collectAsState()
    var text by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    
    val primaryColor = if (mode == OffGridMode.EMERGENCY) Color(0xFFFF4500) else Color(0xFF00B4FF)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Mesh Chat", fontWeight = FontWeight.Bold) },
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
                    ChatBubble(msg, isMe = msg.sender == nearbyManager.userName, primaryColor)
                }
            }

            // Quick Actions Bar
            if (mode == OffGridMode.FESTIVAL) {
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
                        placeholder = { Text("Type an offline message...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                nearbyManager.sendMessage(text)
                                text = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
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
        
        Text(
            text = if (msg.hops == 0) "direct" else "via ${msg.hops} nodes",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}
