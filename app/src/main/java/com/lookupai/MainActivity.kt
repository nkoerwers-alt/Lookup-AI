package com.lookupai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

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

    var apiKey by remember {
        mutableStateOf("")
    }

    var connected by remember {
        mutableStateOf(false)
    }

    val viewModel = remember {
        ChatViewModel()
    }

    MaterialTheme {

        if (!connected) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "Lookup AI",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "Connect your AI",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 20.dp
                    )
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                    },
                    label = {
                        Text("API key")
                    },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        connected = true
                    },
                    enabled = apiKey.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("Continue")
                }
            }

        } else {

            ChatScreen(
                viewModel = viewModel,
                apiKey = apiKey
            )
        }
    }
}