package com.lookupai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette
private val Background = Color(0xFF0E1116)
private val Surface = Color(0xFF1A1F27)
private val Accent = Color(0xFF6C8CFF)
private val TextPrimary = Color.White
private val TextMuted = Color(0xFF8B94A3)
private val ErrorColor = Color(0xFFFF6B6B)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var input by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun send() {
        if (input.isNotBlank() && !loading) {
            viewModel.askAI(input)
            input = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Lookup AI",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty() && !loading) {
                Text(
                    text = "Ask me anything",
                    color = TextMuted,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }

                if (loading) {
                    item {
                        CircularProgressIndicator(
                            color = Accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (error != null) {
                    item {
                        Text(
                            text = error ?: "",
                            color = ErrorColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask anything...", color = TextMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Color(0xFF2B323D)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                enabled = input.isNotBlank() && !loading,
                onClick = { send() },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White,
                    disabledContainerColor = Surface,
                    disabledContentColor = TextMuted
                )
            ) {
                Text("Send", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
