package com.lookupai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AIClient {

    private const val API_URL = "https://api.openai.com/v1/chat/completions"

    suspend fun ask(
        apiKey: String,
        question: String
    ): String = withContext(Dispatchers.IO) {

        val connection =
            (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true

                setRequestProperty(
                    "Authorization",
                    "Bearer $apiKey"
                )

                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
            }

        val request = JSONObject().apply {
            put("model", "gpt-4o-mini")

            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", question)
                    }
                )
            )
        }

        connection.outputStream.use { output ->
            output.write(
                request.toString().toByteArray(Charsets.UTF_8)
            )
        }

        val responseCode = connection.responseCode

        val stream =
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val response = stream.bufferedReader().use {
            it.readText()
        }

        if (responseCode !in 200..299) {
            throw Exception(
                "AI request failed ($responseCode)"
            )
        }

        JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}