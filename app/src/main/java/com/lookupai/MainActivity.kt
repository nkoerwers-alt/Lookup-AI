package com.lookupai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LookupAIApp()
        }
    }
}

@Composable
fun LookupAIApp() {
    val viewModel = remember { ChatViewModel() }

    MaterialTheme {
        ChatScreen(
            viewModel = viewModel
        )
    }
}