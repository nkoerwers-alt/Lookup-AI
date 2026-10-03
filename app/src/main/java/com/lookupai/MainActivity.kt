package com.lookupai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    val viewModel = remember {
        ChatViewModel()
    }

    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Lookup AI",
                style = MaterialTheme.typography.headlineLarge
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}