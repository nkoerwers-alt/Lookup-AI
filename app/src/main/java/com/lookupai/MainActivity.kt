package com.lookupai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LookupAI()
        }
    }
}

@Composable
fun LookupAI() {

    var question by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Lookup AI",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = question,
                onValueChange = {
                    question = it
                },
                label = {
                    Text("Ask anything")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                },
                label = {
                    Text("API key")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = question.isNotBlank()
                        && apiKey.isNotBlank()
                        && !loading,
                onClick = {

                    loading = true
                    answer = ""
                    error = ""

                    scope.launch {

                        try {

                            answer = AIClient.ask(
                                apiKey = apiKey,
                                question = question
                            )

                        } catch (e: Exception) {

                            error = e.message
                                ?: "Something went wrong."

                        } finally {

                            loading = false
                        }
                    }
                }
            ) {

                Text(
                    if (loading)
                        "Thinking..."
                    else
                        "Ask Lookup AI"
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            if (loading) {
                CircularProgressIndicator()
            }

            if (error.isNotBlank()) {

                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (answer.isNotBlank()) {

                Text(
                    text = "Answer",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(answer)
            }
        }
    }
}