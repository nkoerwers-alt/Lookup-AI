
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF08080D)
private val SurfaceDark = Color(0xFF15151F)
private val Purple = Color(0xFF9565FF)
private val TextWhite = Color(0xFFF4F2FA)
private val Muted = Color(0xFF92909F)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, loading) {
        val lastIndex = messages.size - 1
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Purple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Fenn AI",
                    color = TextWhite,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your AI assistant",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = { input = "" }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New chat",
                    tint = TextWhite
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

        // Chat area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    Purple.copy(alpha = 0.16f),
                                    RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Purple,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        Text(
                            "What's on your mind?",
                            color = TextWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Ask Fenn anything. Let's figure it out.",
                            color = Muted,
                            fontSize = 14.sp
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
                                SurfaceDark,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = Purple,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Fenn is thinking...",
                            color = Muted,
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

        // Message input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 8.dp)
                .background(
                    SurfaceDark,
                    RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                textStyle = TextStyle(
                    color = TextWhite,
                    fontSize = 15.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Purple),
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) {
                            Text(
                                "Message Fenn AI...",
                                color = Muted,
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
                        if (input.isNotBlank() && !loading)
                            Purple else Purple.copy(alpha = 0.25f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Text(
            "Fenn AI can make mistakes. Check important information.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = Muted.copy(alpha = 0.65f),
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
