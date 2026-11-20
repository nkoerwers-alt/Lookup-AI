package com.lookupai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val SuggestionPrompts = listOf(
    "Explain something simply",
    "Help me write a message",
    "Brainstorm ideas with me",
    "Plan my day"
)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onGoogleSignIn: suspend () -> Unit,
    onSignOut: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val account by viewModel.account.collectAsState()

    var input by remember { mutableStateOf("") }
    var showOwnerMode by remember { mutableStateOf(false) }
    var showVoiceMode by remember { mutableStateOf(false) }
    var signingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

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

    if (authState == FennAuthState.SIGNED_OUT) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FennColors.Background)
                .padding(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                FennLogo(logoSize = 52.dp, fontSize = 34.sp)

                Spacer(Modifier.height(28.dp))

                Text(
                    "fenn",
                    color = FennColors.Ink,
                    fontSize = 43.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.2).sp
                )

                Text(
                    "A little space to think.",
                    color = FennColors.Muted,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (!signingIn) {
                            signingIn = true
                            loginError = null
                            scope.launch {
                                try {
                                    onGoogleSignIn()
                                } catch (e: Exception) {
                                    loginError =
                                        e.message ?: "Google sign-in failed."
                                } finally {
                                    signingIn = false
                                }
                            }
                        }
                    },
                    enabled = !signingIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FennColors.Ink,
                        contentColor = Color.Black,
                        disabledContainerColor = FennColors.SurfaceHigh
                    )
                ) {
                    if (signingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = FennColors.Ember,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Continue with Google",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.continueAsGuest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FennColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FennColors.Ink
                    )
                ) {
                    Text("Continue as Guest")
                }

                if (loginError != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        loginError ?: "",
                        color = FennColors.Danger,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "Google accounts save your conversations and memories. Guest chats are temporary and are not saved to your account.",
                    color = FennColors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(35.dp))

                OutlinedButton(
                    onClick = { showOwnerMode = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, FennColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FennColors.Muted
                    )
                ) {
                    Text("Owner login")
                }
            }
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight(),
                drawerContainerColor = FennColors.Background,
                drawerContentColor = FennColors.Ink,
                drawerShape = RoundedCornerShape(
                    topEnd = 22.dp,
                    bottomEnd = 22.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FennColors.Background)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (authState == FennAuthState.GUEST)
                                    "GUEST MODE"
                                else
                                    "YOUR CHATS",
                                color = FennColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (authState == FennAuthState.GUEST)
                                    "Temporary chat"
                                else
                                    "History",
                                color = FennColors.Ink,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close menu",
                                tint = FennColors.Muted
                            )
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
                            containerColor = FennColors.Ink,
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

                    if (authState == FennAuthState.GUEST) {
                        Text(
                            "Guest conversations are temporary and won't appear in saved history.",
                            modifier = Modifier.padding(20.dp),
                            color = FennColors.Muted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(Modifier.weight(1f))
                    } else {
                        Text(
                            "RECENT",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = FennColors.Muted,
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
                                        color = FennColors.Muted,
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
                                        selected = currentConversationId ==
                                            conversation.id,
                                        onClick = {
                                            viewModel.openConversation(conversation.id)
                                            input = ""
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = FennColors.SurfaceHigh,
                                            selectedTextColor = FennColors.Amber,
                                            unselectedTextColor = FennColors.Muted
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
                                            tint = FennColors.Muted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = FennColors.Border,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (authState == FennAuthState.GOOGLE) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text(
                                account?.name?.ifBlank { "Google account" }
                                    ?: "Google account",
                                color = FennColors.Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                account?.email ?: "",
                                color = FennColors.Muted,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onSignOut()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, FennColors.Border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = FennColors.Ink
                                )
                            ) {
                                Text("Sign out")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onSignOut()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            border = BorderStroke(1.dp, FennColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = FennColors.Ink
                            )
                        ) {
                            Text("Exit guest mode")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                showOwnerMode = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, FennColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FennColors.Ink
                        )
                    ) {
                        Text(
                            "Log in as owner",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FennColors.Background)
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
                        if (authState == FennAuthState.GOOGLE) {
                            viewModel.loadConversations()
                        }
                        scope.launch { drawerState.open() }
                    }
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Chat menu",
                        tint = FennColors.Ink
                    )
                }

                Spacer(Modifier.width(5.dp))

                FennLogo(logoSize = 35.dp, fontSize = 23.sp)

                Spacer(Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "fenn",
                        color = FennColors.Ink,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.7).sp
                    )
                    Text(
                        if (authState == FennAuthState.GUEST)
                            "Guest mode"
                        else
                            "AI assistant",
                        color = FennColors.Muted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { showVoiceMode = true }
                ) {
                    MicIcon(
                        color = FennColors.Ink,
                        iconSize = 23.dp
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
                        tint = FennColors.Ink
                    )
                }
            }

            HorizontalDivider(color = FennColors.Border)

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
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "A LITTLE SPACE\nTO THINK.",
                                color = FennColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                lineHeight = 19.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            Text(
                                "What's on\nyour mind?",
                                color = FennColors.Ink,
                                fontSize = 39.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 44.sp,
                                letterSpacing = (-1.5).sp
                            )

                            Spacer(Modifier.height(15.dp))

                            Text(
                                "Ideas, questions, problems.\nStart anywhere.",
                                color = FennColors.Muted,
                                fontSize = 15.sp,
                                lineHeight = 23.sp
                            )

                            Spacer(Modifier.height(28.dp))

                            SuggestionPrompts.forEach { prompt ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 9.dp)
                                        .background(
                                            FennColors.Surface,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            1.dp,
                                            FennColors.Border,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable(enabled = !loading) {
                                            viewModel.askAI(prompt)
                                        }
                                        .padding(
                                            horizontal = 16.dp,
                                            vertical = 14.dp
                                        )
                                ) {
                                    Text(
                                        prompt,
                                        color = FennColors.Ink,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .background(
                                            FennColors.Ember,
                                            CircleShape
                                        )
                                )

                                Spacer(Modifier.width(9.dp))

                                Text(
                                    "READY WHEN YOU ARE",
                                    color = FennColors.Muted,
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
                            color = FennColors.Danger,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            val canSend = input.isNotBlank() && !loading

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp)
                    .border(
                        width = 1.dp,
                        color = if (input.isNotBlank())
                            FennColors.Ember.copy(alpha = 0.45f)
                        else
                            FennColors.Border,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(
                        FennColors.Surface,
                        RoundedCornerShape(20.dp)
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
                        color = FennColors.Ink,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(FennColors.Ember),
                    decorationBox = { innerTextField ->
                        Box {
                            if (input.isEmpty()) {
                                Text(
                                    "Message Fenn...",
                                    color = FennColors.Muted,
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
                    enabled = canSend,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = if (canSend)
                                Brush.linearGradient(
                                    listOf(FennColors.Ember, FennColors.Amber)
                                )
                            else
                                SolidColor(FennColors.SurfaceHigh),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send message",
                        tint = if (canSend)
                            FennColors.OnAccent
                        else
                            FennColors.Muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                "Fenn can make mistakes. Check important information.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 9.dp),
                color = FennColors.Muted.copy(alpha = 0.65f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
