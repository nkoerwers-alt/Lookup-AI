
package com.lookupai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val ADMIN_BASE_URL =
    "https://lookup-ai-backend.nkoerwers.workers.dev"

private val OwnerBackground = Color(0xFF101010)
private val OwnerSurface = Color(0xFF1C1C1C)
private val OwnerWhite = Color.White
private val OwnerMuted = Color(0xFF999999)
private val OwnerRed = Color(0xFFE57373)

private data class AdminStats(
    val users: Int,
    val conversations: Int,
    val messages: Int
)

private data class AdminConversation(
    val id: String,
    val userId: String,
    val title: String,
    val createdAt: String
)

private data class AdminMessage(
    val role: String,
    val content: String,
    val createdAt: String
)

private data class AdminConversationDetail(
    val conversation: AdminConversation,
    val messages: List<AdminMessage>
)

private object AdminApi {

    private suspend fun request(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {

        val connection = URL("$ADMIN_BASE_URL$path")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/json")

            if (token != null) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use {
                it.readText()
            } ?: "{}"

            val response = JSONObject(responseText)

            if (status !in 200..299) {
                throw Exception(
                    response.optString(
                        "error",
                        "Server error: $status"
                    )
                )
            }

            response
        } finally {
            connection.disconnect()
        }
    }

    suspend fun login(password: String): String {
        val response = request(
            path = "/admin/login",
            method = "POST",
            body = JSONObject().put("password", password)
        )

        return response.getString("token")
    }

    suspend fun getStats(token: String): AdminStats {
        val response = request(
            "/admin/stats",
            token = token
        )

        return AdminStats(
            users = response.optInt("users"),
            conversations = response.optInt("conversations"),
            messages = response.optInt("messages")
        )
    }

    suspend fun getConversations(
        token: String,
        search: String
    ): List<AdminConversation> {

        val encoded = URLEncoder.encode(
            search,
            "UTF-8"
        )

        val response = request(
            "/admin/conversations?search=$encoded",
            token = token
        )

        val array = response.optJSONArray("conversations")
            ?: JSONArray()

        return List(array.length()) { index ->
            val item = array.getJSONObject(index)

            AdminConversation(
                id = item.optString("id"),
                userId = item.optString("user_id"),
                title = item.optString("title"),
                createdAt = item.optString("created_at")
            )
        }
    }

    suspend fun getConversation(
        token: String,
        id: String
    ): AdminConversationDetail {

        val encoded = URLEncoder.encode(id, "UTF-8")

        val response = request(
            "/admin/conversations/$encoded",
            token = token
        )

        val conversationJson =
            response.getJSONObject("conversation")

        val conversation = AdminConversation(
            id = conversationJson.optString("id"),
            userId = conversationJson.optString("user_id"),
            title = conversationJson.optString("title"),
            createdAt = conversationJson.optString("created_at")
        )

        val array = response.optJSONArray("messages")
            ?: JSONArray()

        val messages = List(array.length()) { index ->
            val item = array.getJSONObject(index)

            AdminMessage(
                role = item.optString("role"),
                content = item.optString("content"),
                createdAt = item.optString("created_at")
            )
        }

        return AdminConversationDetail(
            conversation = conversation,
            messages = messages
        )
    }

    suspend fun deleteConversation(
        token: String,
        id: String
    ) {
        val encoded = URLEncoder.encode(id, "UTF-8")

        request(
            path = "/admin/conversations/$encoded",
            method = "DELETE",
            token = token
        )
    }
}

@Composable
fun OwnerMode(
    onClose: () -> Unit = {}
) {
    var adminToken by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = OwnerBackground
    ) {
        if (adminToken == null) {
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
                    "Secure Fenn AI administration",
                    color = OwnerMuted,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Owner password") },
                    singleLine = true,
                    enabled = !loading,
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

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = OwnerRed)
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (password.isBlank()) {
                            error = "Enter your owner password."
                            return@Button
                        }

                        loading = true
                        error = ""

                        scope.launch {
                            try {
                                adminToken =
                                    AdminApi.login(password)
                                password = ""
                            } catch (e: Exception) {
                                error = e.message ?: "Login failed."
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OwnerWhite,
                        contentColor = Color.Black
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Unlock Owner Mode")
                    }
                }

                TextButton(onClick = onClose) {
                    Text("Back to Fenn AI", color = OwnerMuted)
                }
            }
        } else {
            OwnerDashboard(
                token = adminToken!!,
                onLogout = {
                    adminToken = null
                    password = ""
                },
                onClose = onClose
            )
        }
    }
}

@Composable
private fun OwnerDashboard(
    token: String,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var conversations by remember {
        mutableStateOf<List<AdminConversation>>(emptyList())
    }
    var selected by remember {
        mutableStateOf<AdminConversationDetail?>(null)
    }

    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            loading = true
            error = ""

            try {
                stats = AdminApi.getStats(token)
                conversations =
                    AdminApi.getConversations(token, search)
            } catch (e: Exception) {
                error = e.message ?: "Could not load dashboard."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(token) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OwnerBackground)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "f.",
                    color = OwnerWhite,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Owner Dashboard",
                    color = OwnerWhite,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Fenn AI • Created by Vibe",
                    color = OwnerMuted,
                    fontSize = 12.sp
                )
            }

            TextButton(onClick = onLogout) {
                Text("Log out", color = OwnerWhite)
            }
        }

        Spacer(Modifier.height(18.dp))

        if (error.isNotBlank()) {
            Text(
                error,
                color = OwnerRed,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        if (notice.isNotBlank()) {
            Text(
                notice,
                color = OwnerWhite,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        if (selected != null) {
            val detail = selected!!

            TextButton(
                onClick = {
                    selected = null
                    confirmDelete = false
                }
            ) {
                Text("← All conversations", color = OwnerWhite)
            }

            Text(
                detail.conversation.title,
                color = OwnerWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "User ID: ${detail.conversation.userId}",
                color = OwnerMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))

            if (confirmDelete) {
                Text(
                    "Permanently delete this conversation?",
                    color = OwnerRed
                )

                Row {
                    TextButton(
                        onClick = {
                            confirmDelete = false
                        }
                    ) {
                        Text("Cancel", color = OwnerMuted)
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    AdminApi.deleteConversation(
                                        token,
                                        detail.conversation.id
                                    )
                                    selected = null
                                    confirmDelete = false
                                    notice = "Conversation deleted."
                                    refresh()
                                } catch (e: Exception) {
                                    error = e.message ?: "Delete failed."
                                }
                            }
                        }
                    ) {
                        Text("Confirm delete", color = OwnerRed)
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        confirmDelete = true
                    }
                ) {
                    Text("Delete conversation", color = OwnerRed)
                }
            }

            Divider(color = Color.DarkGray)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(detail.messages) { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.role == "user") {
                                Color(0xFF252525)
                            } else {
                                OwnerSurface
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                if (message.role == "user") "USER" else "FENN AI",
                                color = OwnerMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                message.content,
                                color = OwnerWhite,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "OVERVIEW",
                color = OwnerMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    "Users",
                    stats?.users?.toString() ?: "—",
                    Modifier.weight(1f)
                )
                StatCard(
                    "Chats",
                    stats?.conversations?.toString() ?: "—",
                    Modifier.weight(1f)
                )
                StatCard(
                    "Messages",
                    stats?.messages?.toString() ?: "—",
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "ALL CONVERSATIONS",
                color = OwnerMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search title or user ID") },
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

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        notice = ""
                        refresh()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OwnerWhite,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Search")
                }

                OutlinedButton(
                    onClick = {
                        search = ""
                        notice = ""
                        refresh()
                    }
                ) {
                    Text("Clear", color = OwnerWhite)
                }

                OutlinedButton(onClick = { refresh() }) {
                    Text("Refresh", color = OwnerWhite)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = OwnerWhite
                )
            }

            if (!loading && conversations.isEmpty()) {
                Text(
                    "No conversations found.",
                    color = OwnerMuted,
                    fontSize = 13.sp
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(conversations) { conversation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    loading = true
                                    error = ""

                                    try {
                                        selected =
                                            AdminApi.getConversation(
                                                token,
                                                conversation.id
                                            )
                                    } catch (e: Exception) {
                                        error = e.message
                                            ?: "Could not open conversation."
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = OwnerSurface
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(15.dp)
                        ) {
                            Text(
                                conversation.title.ifBlank {
                                    "Untitled conversation"
                                },
                                color = OwnerWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                "User: ${conversation.userId}",
                                color = OwnerMuted,
                                fontSize = 10.sp
                            )

                            Text(
                                "ID: ${conversation.id}",
                                color = OwnerMuted,
                                fontSize = 10.sp
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                "Tap to open →",
                                color = OwnerWhite,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
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
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = OwnerSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                title,
                color = OwnerMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(5.dp))

            Text(
                value,
                color = OwnerWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
