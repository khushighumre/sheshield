package com.example.sheshield.data.supabase

import android.content.Context
import android.util.Log
import com.example.sheshield.data.Crypto
import com.example.sheshield.data.SosAlertStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class CloudRepository(private val context: Context) {

    companion object {
        private const val TAG = "SheShieldCloud"
    }

    private fun postRest(endpoint: String, jsonBody: String, authToken: String?): Boolean {
        if (SupabaseClient.SUPABASE_URL.isBlank() || SupabaseClient.SUPABASE_URL.contains("your-supabase-url")) {
            Log.w(TAG, "Supabase URL not configured, skipping sync.")
            return false
        }
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${SupabaseClient.SUPABASE_URL}/rest/v1/$endpoint")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                if (!authToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                return true
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "Supabase REST error on $endpoint: HTTP $responseCode - $err")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception syncing to $endpoint", e)
            return false
        } finally {
            conn?.disconnect()
        }
    }

    fun syncPendingData() {
        val authToken = SupabaseAuthManager.getAccessToken(context) ?: SupabaseClient.getAuthToken(context)
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        // 1. Sync local incidents from encrypted vault files
        val vaultDir = File(context.filesDir, "vault")
        if (vaultDir.exists()) {
            val incidentFiles = vaultDir.listFiles { _, name -> name.startsWith("incident_") && name.endsWith(".enc") }
            incidentFiles?.forEach { file ->
                try {
                    val encryptedBytes = file.readBytes()
                    val decryptedBytes = Crypto.decrypt(encryptedBytes)
                    val jsonStr = String(decryptedBytes, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)

                    val id = json.getString("id")
                    val type = json.getString("type")
                    val startedAt = json.getLong("startedAt")
                    val endedAt = if (json.has("endedAt") && !json.isNull("endedAt")) json.getLong("endedAt") else null
                    val status = if (endedAt != null) "RESOLVED" else "ACTIVE"
                    val notes = json.optString("notes", "")

                    val incidentObj = JSONObject().apply {
                        put("id", id)
                        put("device_id", deviceId)
                        put("incident_type", type)
                        put("started_at", startedAt)
                        if (endedAt != null) put("ended_at", endedAt)
                        put("status", status)
                        put("notes", notes)
                        put("is_remote_alert", false)
                    }

                    if (postRest("incidents", incidentObj.toString(), authToken)) {
                        // Sync points array
                        val pointsArr = json.optJSONArray("points")
                        if (pointsArr != null && pointsArr.length() > 0) {
                            val pointsPayload = JSONArray()
                            for (i in 0 until pointsArr.length()) {
                                val p = pointsArr.getJSONObject(i)
                                val lat = if (p.has("lat") && !p.isNull("lat")) p.getDouble("lat") else JSONObject.NULL
                                val lng = if (p.has("lng") && !p.isNull("lng")) p.getDouble("lng") else JSONObject.NULL
                                val time = p.getLong("time")

                                val ptObj = JSONObject().apply {
                                    put("incident_id", id)
                                    put("lat", lat)
                                    put("lng", lng)
                                    put("recorded_at", time)
                                }
                                pointsPayload.put(ptObj)
                            }
                            postRest("incident_points", pointsPayload.toString(), authToken)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process incident file for sync: ${file.name}", e)
                }
            }
        }

        // 2. Sync received P2P SOS alerts
        val alertStore = SosAlertStore(context)
        val alerts = alertStore.loadAlerts()
        if (alerts.isNotEmpty()) {
            val p2pPayload = JSONArray()
            for (alert in alerts) {
                val msgObj = JSONObject().apply {
                    put("message_id", alert.messageId)
                    put("incident_id", alert.incidentId)
                    put("sender_id", alert.senderId)
                    put("message_type", if (alert.isSafe) "SAFE" else "SOS")
                    put("message_text", alert.messageText)
                    put("lat", alert.lat ?: JSONObject.NULL)
                    put("lng", alert.lng ?: JSONObject.NULL)
                    put("wall_time", alert.receivedAt)
                    put("counter", 0)
                    put("hop_count", 0)
                    put("is_forwarded", true)
                }
                p2pPayload.put(msgObj)
            }
            postRest("p2p_messages", p2pPayload.toString(), authToken)
        }

        Log.i(TAG, "Cloud synchronization pass finished.")
    }
}
