package com.example.sheshield.data

import android.util.Log
import com.example.sheshield.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object OpenRouterClient {
    private const val TAG = "OpenRouterClient"
    private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

    suspend fun getSafetySummary(prompt: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "OpenRouter API key is blank")
            return@withContext null
        }

        try {
            val url = URL(ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://github.com/codova01/SheShield")
                setRequestProperty("X-Title", "SheShield")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val jsonBody = JSONObject().apply {
                put("model", "openai/gpt-4o-mini")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody.toString()) }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseString)
                val choices = root.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    return@withContext message.getString("content").trim()
                }
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "unknown"
                Log.e(TAG, "OpenRouter error: $responseCode - $errorStr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling OpenRouter", e)
        }
        return@withContext null
    }
}
