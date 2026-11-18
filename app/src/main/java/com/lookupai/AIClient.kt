
package com.lookupai

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class AIReply(
    val answer: String,
    val conversationId: String
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

    private fun request(
        path: String,
        body: JSONObject,
        token: String? = null
    ): Pair<Int, String> {
        val connection =
            (URL("$API_URL$path").openConnection() as HttpURLConnection)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 120000
            connection.doOutput = true

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

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode

            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream.bufferedReader().use {
                it.readText()
            }

            return Pair(code, response)
        } finally {
            connection.disconnect()
        }
    }

    private fun createSession(): String {
        val result = request(
            "/session",
            JSONObject()
        )

        if (result.first !in 200..299) {
            throw Exception("Could not create Fenn session.")
        }

        val token = JSONObject(result.second).getString("token")

        preferences().edit()
            .putString(TOKEN_KEY, token)
            .apply()

        return token
    }

    private fun getSession(): String {
        return preferences().getString(TOKEN_KEY, null)
            ?: createSession()
    }

    suspend fun ask(
        question: String,
        conversationId: String
    ): AIReply = withContext(Dispatchers.IO) {

        val token = getSession()

        val body = JSONObject().apply {
            put("question", question)
            put("conversationId", conversationId)
        }

        val result = request(
            "/chat",
            body,
            token
        )

        if (result.first !in 200..299) {
            val error = try {
                JSONObject(result.second)
                    .optString("error", "AI request failed.")
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

    suspend fun getMemories(): List<String> =
        withContext(Dispatchers.IO) {
            val token = getSession()

            val connection =
                URL("$API_URL/memory").openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 30000

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream.bufferedReader().use {
                    it.readText()
                }

                if (code !in 200..299) {
                    throw Exception("Could not load memories.")
                }

                val array = JSONObject(response)
                    .getJSONArray("memories")

                List(array.length()) {
                    array.getJSONObject(it).getString("content")
                }
            } finally {
                connection.disconnect()
            }
        }

    suspend fun deleteMemories() =
        withContext(Dispatchers.IO) {
            val token = getSession()

            val connection =
                URL("$API_URL/memory").openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "DELETE"
                connection.connectTimeout = 20000
                connection.readTimeout = 30000

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )

                if (connection.responseCode !in 200..299) {
                    throw Exception("Could not delete memories.")
                }
            } finally {
                connection.disconnect()
            }
        }
}
