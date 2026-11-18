
package com.lookupai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FennBackground = Color(0xFF08080D)
private val FennSurface = Color(0xFF171720)
private val FennPurple = Color(0xFF9565FF)
private val FennWhite = Color(0xFFF4F2FA)
private val FennMuted = Color(0xFF92909F)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, loading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FennBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(FennPurple, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "F",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Fenn AI",
                    color = FennWhite,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your AI assistant",
                    color = FennMuted,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = { input = "" }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New chat",
                    tint = FennWhite
                )
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f)
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 75.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(
                                    FennPurple.copy(alpha = 0.16f),
                                    RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "F",
                                color = FennPurple,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        Text(
                            "What's on your mind?",
                            color = FennWhite,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Ask Fenn anything. Let's figure it out.",
                            color = FennMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(messages) { message ->
                MessageBubble(message)
            }

            if (loading) {
                item {
                    Row(
                        modifier = Modifier
                            .background(
                                FennSurface,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = FennPurple,
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            "Fenn is thinking...",
                            color = FennMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        error ?: "",
                        color = Color(0xFFFF7777),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp)
                .background(
                    FennSurface,
                    RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 7.dp, vertical = 10.dp),
                textStyle = TextStyle(
                    color = FennWhite,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(FennPurple),
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) {
                            Text(
                                "Message Fenn AI...",
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
                    .size(44.dp)
                    .background(
                        FennPurple.copy(
                            alpha = if (input.isNotBlank() && !loading)
                                1f else 0.35f
                        ),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }

        Text(
            "Fenn AI can make mistakes. Check important information.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = FennMuted.copy(alpha = 0.65f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}
