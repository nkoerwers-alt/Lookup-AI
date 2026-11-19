
package com.lookupai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val FennBackground = Color(0xFF101010)
private val FennSurface = Color(0xFF191919)
private val FennSurfaceLight = Color(0xFF222222)
private val FennWhite = Color(0xFFF5F5F5)
private val FennMuted = Color(0xFF929292)
private val FennBorder = Color(0xFF303030)
private val FennDanger = Color(0xFFE57373)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()

    var input by remember { mutableStateOf("") }
    var showOwnerMode by remember { mutableStateOf(false) }
    var showVoiceMode by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, loading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                if (loading) messages.size else messages.lastIndex
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    if (showOwnerMode) {
        OwnerMode(onClose = { showOwnerMode = false })
        return
    }

    if (showVoiceMode) {
        FennVoiceMode(
            viewModel = viewModel,
            onClose = { showVoiceMode = false }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight(),
                drawerContainerColor = FennBackground,
                drawerContentColor = FennWhite,
                drawerShape = RoundedCornerShape(
                    topEnd = 22.dp,
                    bottomEnd = 22.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FennBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "YOUR CHATS",
                                color = FennMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "History",
                                color = FennWhite,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        ) {
                            Text("✕", color = FennMuted, fontSize = 19.sp)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.clearChat()
                            input = ""
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FennWhite,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "New conversation",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Text(
                        "RECENT",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = FennMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            bottom = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (conversations.isEmpty()) {
                            item {
                                Text(
                                    "Your conversations will appear here.",
                                    modifier = Modifier.padding(10.dp),
                                    color = FennMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        items(
                            items = conversations,
                            key = { it.id }
                        ) { conversation ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            conversation.title.ifBlank {
                                                "New conversation"
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 13.sp
                                        )
                                    },
                                    selected = currentConversationId == conversation.id,
                                    onClick = {
                                        viewModel.openConversation(conversation.id)
                                        input = ""
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = FennSurfaceLight,
                                        selectedTextColor = FennWhite,
                                        unselectedTextColor = FennMuted
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        viewModel.deleteConversation(conversation.id)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete conversation",
                                        tint = FennMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = FennBorder,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                showOwnerMode = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            FennBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FennWhite
                        )
                    ) {
                        Text(
                            "♛   Log in as owner",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FennBackground)
                .padding(horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.loadConversations()
                        scope.launch { drawerState.open() }
                    }
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Chat history",
                        tint = FennWhite
                    )
                }

                Spacer(Modifier.width(5.dp))

                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(FennWhite, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "f.",
                        color = Color.Black,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "fenn",
                        color = FennWhite,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.7).sp
                    )
                    Text(
                        "AI assistant",
                        color = FennMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { showVoiceMode = true }
                ) {
                    Text(
                        "🎙",
                        fontSize = 23.sp
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.clearChat()
                        input = ""
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New chat",
                        tint = FennWhite
                    )
                }
            }

            HorizontalDivider(color = FennBorder)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 85.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "A LITTLE SPACE\nTO THINK.",
                                color = FennMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                lineHeight = 19.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            Text(
                                "What's on\nyour mind?",
                                color = FennWhite,
                                fontSize = 39.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 44.sp,
                                letterSpacing = (-1.5).sp
                            )

                            Spacer(Modifier.height(15.dp))

                            Text(
                                "Ideas, questions, problems.\nStart anywhere.",
                                color = FennMuted,
                                fontSize = 15.sp,
                                lineHeight = 23.sp
                            )

                            Spacer(Modifier.height(30.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .background(
                                            Color(0xFFAAAAAA),
                                            CircleShape
                                        )
                                )

                                Spacer(Modifier.width(9.dp))

                                Text(
                                    "READY WHEN YOU ARE",
                                    color = FennMuted,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }
                }

                items(messages) { message ->
                    MessageBubble(message)
                }

                if (loading) {
                    item(key = "fenn_thinking") {
                        FennAIThinking(
                            cubeSize = 30.dp,
                            name = "fenn",
                            message = " is thinking..."
                        )
                    }
                }

                if (error != null) {
                    item {
                        Text(
                            error ?: "",
                            color = FennDanger,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp)
                    .border(
                        width = 1.dp,
                        color = FennBorder,
                        shape = RoundedCornerShape(19.dp)
                    )
                    .background(
                        FennSurface,
                        RoundedCornerShape(19.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 9.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        color = FennWhite,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(FennWhite),
                    decorationBox = { innerTextField ->
                        Box {
                            if (input.isEmpty()) {
                                Text(
                                    "Message Fenn...",
                                    color = FennMuted,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(
                    onClick = {
                        if (input.isNotBlank() && !loading) {
                            viewModel.askAI(input.trim())
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && !loading,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (input.isNotBlank() && !loading)
                                FennWhite
                            else FennSurfaceLight,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send message",
                        tint = if (input.isNotBlank() && !loading)
                            Color.Black
                        else FennMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                "Fenn can make mistakes. Check important information.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 9.dp),
                color = FennMuted.copy(alpha = 0.65f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
