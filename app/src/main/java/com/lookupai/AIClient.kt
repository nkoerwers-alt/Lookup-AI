
package com.lookupai

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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

object AIClient {

    private const val API_URL =
        "https://lookup-ai-backend.nkoerwers.workers.dev"

    private const val PREFS = "fenn_session"
    private const val TOKEN_KEY = "session_token"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun preferences() =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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

    private suspend fun getSession(): String {
        val saved = preferences().getString(TOKEN_KEY, null)
        if (!saved.isNullOrBlank()) return saved

        val result = request(
            path = "/session",
            method = "POST",
            body = JSONObject()
        )

        if (result.first !in 200..299) {
            val message = try {
                JSONObject(result.second).optString(
                    "error",
                    "Could not create Fenn session."
                )
            } catch (_: Exception) {
                "Could not create Fenn session. Server returned ${result.first}."
            }

            throw Exception("$message (HTTP ${result.first})")
        }

        val token = JSONObject(result.second).getString("token")

        preferences().edit()
            .putString(TOKEN_KEY, token)
            .apply()

        return token
    }

    suspend fun ask(
        question: String,
        conversationId: String
    ): AIReply {
        val token = getSession()

        val body = JSONObject().apply {
            put("question", question)
            put("conversationId", conversationId)
        }

        val result = request(
            path = "/chat",
            method = "POST",
            body = body,
            token = token
        )

        if (result.first !in 200..299) {
            val error = try {
                JSONObject(result.second).optString(
                    "error",
                    "AI request failed."
                )
            } catch (_: Exception) {
                "AI request failed."
            }

            throw Exception(error)
        }

        val data = JSONObject(result.second)

        AIReply(
            answer = data.getString("answer"),
            conversationId = data.getString("conversationId")
        )
    }

    suspend fun getConversations(): List<SavedConversation> {
        val token = getSession()
        val result = request(
            path = "/conversations",
            token = token
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
        val token = getSession()
        val result = request(
            path = "/conversations/${java.net.URLEncoder.encode(id, "UTF-8")}",
            token = token
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
        val token = getSession()
        val result = request(
            path = "/conversations/${java.net.URLEncoder.encode(id, "UTF-8")}",
            method = "DELETE",
            token = token
        )

        if (result.first !in 200..299) {
            throw Exception("Could not delete conversation.")
        }
    }

    suspend fun getMemories(): List<String> {
        val token = getSession()
        val result = request(
            path = "/memory",
            token = token
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
        val token = getSession()
        val result = request(
            path = "/memory",
            method = "DELETE",
            token = token
        )

        if (result.first !in 200..299) {
            throw Exception("Could not delete memories.")
        }
    }
}
