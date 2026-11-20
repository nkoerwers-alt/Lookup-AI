
package com.lookupai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AIReply(
    val answer: String,
    val conversationId: String
)

data class SavedConversation(
    val id: String,
    val title: String,
    val createdAt: String
)

data class LoadedConversation(
    val id: String,
    val messages: List<ChatMessage>
)

data class FennAccount(
    val email: String,
    val name: String,
    val picture: String
)

object AIClient {

    private const val API_URL =
        "https://lookup-ai-backend.nkoerwers.workers.dev"

    private const val PREFS = "fenn_session"
    private const val TOKEN_KEY = "google_session_token"
    private const val GUEST_KEY = "guest_mode"
    private const val EMAIL_KEY = "account_email"
    private const val NAME_KEY = "account_name"
    private const val PICTURE_KEY = "account_picture"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun preferences() =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isGuest(): Boolean =
        preferences().getBoolean(GUEST_KEY, false)

    fun isSignedIn(): Boolean =
        !preferences().getString(TOKEN_KEY, null).isNullOrBlank()

    fun getAccount(): FennAccount? {
        if (!isSignedIn()) return null

        val prefs = preferences()

        return FennAccount(
            email = prefs.getString(EMAIL_KEY, "") ?: "",
            name = prefs.getString(NAME_KEY, "") ?: "",
            picture = prefs.getString(PICTURE_KEY, "") ?: ""
        )
    }

    fun startGuestMode() {
        preferences().edit()
            .remove(TOKEN_KEY)
            .remove(EMAIL_KEY)
            .remove(NAME_KEY)
            .remove(PICTURE_KEY)
            .putBoolean(GUEST_KEY, true)
            .apply()
    }

    fun signOut() {
        preferences().edit()
            .clear()
            .apply()
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {

        val connection =
            URL("$API_URL$path").openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 20000
            connection.readTimeout = 120000

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            if (token != null) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val code = connection.responseCode

            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use {
                it.readText()
            } ?: ""

            Pair(code, response)

        } finally {
            connection.disconnect()
        }
    }

    suspend fun signInWithGoogle(idToken: String): FennAccount {
        val body = JSONObject().apply {
            put("idToken", idToken)
        }

        val result = request(
            path = "/auth/google",
            method = "POST",
            body = body
        )

        if (result.first !in 200..299) {
            val message = try {
                JSONObject(result.second).optString(
                    "error",
                    "Google sign-in failed."
                )
            } catch (_: Exception) {
                "Google sign-in failed."
            }

            throw Exception(message)
        }

        val data = JSONObject(result.second)
        val accountData = data.getJSONObject("account")
        val token = data.getString("token")

        val account = FennAccount(
            email = accountData.optString("email", ""),
            name = accountData.optString("name", ""),
            picture = accountData.optString("picture", "")
        )

        preferences().edit()
            .putString(TOKEN_KEY, token)
            .putString(EMAIL_KEY, account.email)
            .putString(NAME_KEY, account.name)
            .putString(PICTURE_KEY, account.picture)
            .putBoolean(GUEST_KEY, false)
            .apply()

        return account
    }

    private fun requireGoogleToken(): String {
        if (isGuest()) {
            throw Exception("This feature requires a Google account.")
        }

        return preferences().getString(TOKEN_KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?: throw Exception("Please sign in with Google.")
    }

    suspend fun ask(
        question: String,
        conversationId: String,
        previousMessages: List<ChatMessage> = emptyList()
    ): AIReply {

        val body = JSONObject().apply {
            put("question", question)
            put("conversationId", conversationId)

            if (isGuest()) {
                val history = JSONArray()

                previousMessages.takeLast(12).forEach { message ->
                    history.put(JSONObject().apply {
                        put(
                            "role",
                            if (message.isUser) "user" else "assistant"
                        )
                        put("content", message.text)
                    })
                }

                history.put(JSONObject().apply {
                    put("role", "user")
                    put("content", question)
                })

                put("messages", history)
            }
        }

        val result = if (isGuest()) {
            request(
                path = "/guest/chat",
                method = "POST",
                body = body
            )
        } else {
            request(
                path = "/chat",
                method = "POST",
                body = body,
                token = requireGoogleToken()
            )
        }

        if (result.first !in 200..299) {
            val error = try {
                JSONObject(result.second).optString(
                    "error",
                    "AI request failed."
                )
            } catch (_: Exception) {
                "AI request failed. HTTP ${result.first}"
            }

            throw Exception(error)
        }

        val data = JSONObject(result.second)

        return AIReply(
            answer = data.getString("answer"),
            conversationId = data.getString("conversationId")
        )
    }

    suspend fun getConversations(): List<SavedConversation> {
        if (isGuest()) return emptyList()

        val result = request(
            path = "/conversations",
            token = requireGoogleToken()
        )

        if (result.first !in 200..299) {
            throw Exception("Could not load chat history.")
        }

        val array = JSONObject(result.second)
            .getJSONArray("conversations")

        return List(array.length()) { index ->
            val item = array.getJSONObject(index)

            SavedConversation(
                id = item.getString("id"),
                title = item.optString("title", "New chat"),
                createdAt = item.optString("created_at", "")
            )
        }
    }

    suspend fun getConversation(id: String): LoadedConversation {
        val encodedId = URLEncoder.encode(id, "UTF-8")

        val result = request(
            path = "/conversations/$encodedId",
            token = requireGoogleToken()
        )

        if (result.first !in 200..299) {
            throw Exception("Could not open conversation.")
        }

        val data = JSONObject(result.second)
        val conversation = data.getJSONObject("conversation")
        val array = data.getJSONArray("messages")

        val messages = mutableListOf<ChatMessage>()

        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)

            messages.add(
                ChatMessage(
                    text = item.getString("content"),
                    isUser = item.getString("role") == "user"
                )
            )
        }

        return LoadedConversation(
            id = conversation.getString("id"),
            messages = messages
        )
    }

    suspend fun deleteConversation(id: String) {
        val encodedId = URLEncoder.encode(id, "UTF-8")

        val result = request(
            path = "/conversations/$encodedId",
            method = "DELETE",
            token = requireGoogleToken()
        )

        if (result.first !in 200..299) {
            throw Exception("Could not delete conversation.")
        }
    }

    suspend fun getMemories(): List<String> {
        val result = request(
            path = "/memory",
            token = requireGoogleToken()
        )

        if (result.first !in 200..299) {
            throw Exception("Could not load memories.")
        }

        val array = JSONObject(result.second)
            .getJSONArray("memories")

        return List(array.length()) {
            array.getJSONObject(it).getString("content")
        }
    }

    suspend fun deleteMemories() {
        val result = request(
            path = "/memory",
            method = "DELETE",
            token = requireGoogleToken()
        )

        if (result.first !in 200..299) {
            throw Exception("Could not delete memories.")
        }
    }
}
