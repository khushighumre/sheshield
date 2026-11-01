package com.example.sheshield.data.supabase

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupabaseAuthManager {
    private const val TAG = "SheShieldAuth"
    private const val PREFS_NAME = "sheshield_auth"

    fun getAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L)
        // If expired or expiring in < 60 seconds, try to refresh
        if (token != null && System.currentTimeMillis() >= (expiresAt - 60_000L)) {
            val refreshed = refreshToken(context)
            if (refreshed != null) return refreshed
        }
        return token
    }

    fun getUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("user_id", null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return !getAccessToken(context).isNullOrBlank()
    }

    fun signOut(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        SupabaseClient.setAuthToken(context, null)
    }

    private fun saveSession(context: Context, jsonResponse: JSONObject) {
        try {
            val accessToken = jsonResponse.optString("access_token", "")
            val refreshToken = jsonResponse.optString("refresh_token", "")
            val expiresIn = jsonResponse.optLong("expires_in", 3600L)
            val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
            val userObj = jsonResponse.optJSONObject("user")
            val userId = userObj?.optString("id", "") ?: ""

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putLong("expires_at", expiresAt)
                .putString("user_id", userId)
                .apply()

            SupabaseClient.setAuthToken(context, accessToken)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session", e)
        }
    }

    fun signIn(context: Context, email: String, pass: String): Result<String> {
        if (SupabaseClient.SUPABASE_URL.isBlank() || SupabaseClient.SUPABASE_URL.contains("your-supabase-url")) {
            return Result.failure(Exception("Supabase URL not configured."))
        }
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${SupabaseClient.SUPABASE_URL}/auth/v1/token?grant_type=password")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val responseStr = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                return Result.failure(Exception("Login failed: HTTP $responseCode - $err"))
            }

            val json = JSONObject(responseStr)
            saveSession(context, json)
            return Result.success("Successfully logged in.")
        } catch (e: Exception) {
            Log.e(TAG, "Sign in exception", e)
            return Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    fun signUp(context: Context, email: String, pass: String): Result<String> {
        if (SupabaseClient.SUPABASE_URL.isBlank() || SupabaseClient.SUPABASE_URL.contains("your-supabase-url")) {
            return Result.failure(Exception("Supabase URL not configured."))
        }
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${SupabaseClient.SUPABASE_URL}/auth/v1/signup")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseStr)
                // If signup returns access token immediately or needs confirmation
                if (json.has("access_token") && !json.isNull("access_token")) {
                    saveSession(context, json)
                    return Result.success("Account created and logged in.")
                }
                return Result.success("Account created successfully. Please check your email for confirmation if required.")
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                return Result.failure(Exception("Sign up failed: HTTP $responseCode - $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up exception", e)
            return Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun refreshToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", null) ?: return null

        var conn: HttpURLConnection? = null
        try {
            val url = URL("${SupabaseClient.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val body = JSONObject().apply {
                put("refresh_token", refreshToken)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseStr)
                saveSession(context, json)
                return json.optString("access_token", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed", e)
        } finally {
            conn?.disconnect()
        }
        return null
    }
}
