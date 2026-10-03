package com.lookupai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AIClient {

    private const val API_URL =
        "https://lookup-ai-backend.nkoerwers.workers.dev"

    suspend fun ask(question: String): String = withContext(Dispatchers.IO) {
        val connection =
            (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

        val request = JSONObject().apply {
            put("question", question)
        }

        connection.outputStream.use { output ->
            output.write(request.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val stream =
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val response = stream.bufferedReader().use { it.readText() }

        if (responseCode !in 200..299) {
            throw Exception(
                JSONObject(response).optString(
                    "error",
                    "AI request failed."
                )
            )
        }

        JSONObject(response).getString("answer")
    }
}