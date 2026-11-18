
package com.lookupai

import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dark Android system bars
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // White system icons on dark backgrounds
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0

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
