
package com.lookupai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

private const val OWNER_PASSWORD_HASH =
    "c9c8749eb648406ac6983d84073f06b6af9892e6ccc7d88d95407e88dbb99866"

private val OwnerBackground = Color(0xFF101010)
private val OwnerSurface = Color(0xFF1C1C1C)
private val OwnerWhite = Color.White
private val OwnerMuted = Color(0xFF999999)

private fun verifyOwnerPassword(input: String): Boolean {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))

    val enteredHash = digest.joinToString("") {
        "%02x".format(it.toInt() and 0xff)
    }

    return MessageDigest.isEqual(
        enteredHash.toByteArray(Charsets.UTF_8),
        OWNER_PASSWORD_HASH.toByteArray(Charsets.UTF_8)
    )
}

@Composable
fun OwnerMode(
    onClose: () -> Unit = {}
) {
    var loggedIn by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = OwnerBackground
    ) {
        if (!loggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "f.",
                    color = OwnerWhite,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Owner Login",
                    color = OwnerWhite,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Sign in to access Fenn AI owner tools",
                    color = OwnerMuted,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Owner password") },
                    singleLine = true,
                    visualTransformation =
                        PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OwnerWhite,
                        unfocusedTextColor = OwnerWhite,
                        focusedBorderColor = OwnerWhite,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = OwnerWhite,
                        unfocusedLabelColor = OwnerMuted,
                        cursorColor = OwnerWhite
                    )
                )

                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Incorrect password.",
                        color = Color(0xFFFF7777)
                    )
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (verifyOwnerPassword(password)) {
                            loggedIn = true
                            error = false
                            password = ""
                        } else {
                            error = true
                            password = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OwnerWhite,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Unlock Owner Mode")
                }

                TextButton(onClick = onClose) {
                    Text(
                        "Back to Fenn AI",
                        color = OwnerMuted
                    )
                }
            }
        } else {
            OwnerDashboard(
                onLogout = {
                    loggedIn = false
                    password = ""
                },
                onClose = onClose
            )
        }
    }
}

@Composable
private fun OwnerDashboard(
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Text(
            "f.",
            color = OwnerWhite,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Owner Dashboard",
            color = OwnerWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Fenn AI • Created by Vibe",
            color = OwnerMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(25.dp))

        Text(
            "OWNER CONTROLS",
            color = OwnerMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        OwnerCard(
            "User management",
            "User statistics and account management"
        )

        OwnerCard(
            "Conversation management",
            "Review and manage conversations"
        )

        OwnerCard(
            "Memory management",
            "Manage Fenn AI saved memories"
        )

        OwnerCard(
            "AI personality",
            "Configure Fenn AI's personality"
        )

        OwnerCard(
            "System prompt",
            "Manage the assistant's instructions"
        )

        OwnerCard(
            "Usage statistics",
            "Monitor messages and AI usage"
        )

        OwnerCard(
            "Owner settings",
            "Manage owner preferences"
        )

        Spacer(Modifier.height(25.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = OwnerSurface,
                contentColor = OwnerWhite
            )
        ) {
            Text("Log out of Owner Mode")
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Fenn AI", color = OwnerMuted)
        }
    }
}

@Composable
private fun OwnerCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = OwnerSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            Text(
                title,
                color = OwnerWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                description,
                color = OwnerMuted,
                fontSize = 12.sp
            )
        }
    }
}
