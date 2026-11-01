package com.example.sheshield.data

import android.content.Context
import com.example.sheshield.data.p2p.MessageTrustState
import com.example.sheshield.data.p2p.MessageType
import com.example.sheshield.data.p2p.P2pMessage
import org.json.JSONArray
import org.json.JSONObject

data class SosAlertRecord(
    val messageId: String,
    val incidentId: String,
    val senderId: String,
    val messageText: String,
    val lat: Double?,
    val lng: Double?,
    val receivedAt: Long,
    var isSafe: Boolean = false,
    val trustState: String = "VERIFIED_TRUSTED"
) {
    fun toJson(): JSONObject = JSONObject()
        .put("messageId", messageId)
        .put("incidentId", incidentId)
        .put("senderId", senderId)
        .put("messageText", messageText)
        .put("lat", lat ?: JSONObject.NULL)
        .put("lng", lng ?: JSONObject.NULL)
        .put("receivedAt", receivedAt)
        .put("isSafe", isSafe)
        .put("trustState", trustState)

    companion object {
        fun fromJson(json: JSONObject): SosAlertRecord = SosAlertRecord(
            messageId = json.getString("messageId"),
            incidentId = json.getString("incidentId"),
            senderId = json.getString("senderId"),
            messageText = json.getString("messageText"),
            lat = if (json.has("lat") && !json.isNull("lat")) json.getDouble("lat") else null,
            lng = if (json.has("lng") && !json.isNull("lng")) json.getDouble("lng") else null,
            receivedAt = json.getLong("receivedAt"),
            isSafe = json.optBoolean("isSafe", false),
            trustState = json.optString("trustState", "VERIFIED_TRUSTED")
        )
    }
}

class SosAlertStore(context: Context) {
    private val prefs = context.getSharedPreferences("sheshield_prefs", Context.MODE_PRIVATE)

    fun loadAlerts(): List<SosAlertRecord> {
        val raw = prefs.getString("sos_alerts", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            SosAlertRecord.fromJson(arr.getJSONObject(it))
        }
    }

    fun saveAlert(message: P2pMessage, trustState: MessageTrustState = MessageTrustState.VERIFIED_TRUSTED) {
        val alerts = loadAlerts().toMutableList()
        val existingIndex = alerts.indexOfFirst { it.messageId == message.messageId || it.incidentId == message.incidentId }

        if (message.type == MessageType.SAFE) {
            var found = false
            for (i in alerts.indices) {
                if (alerts[i].incidentId == message.incidentId) {
                    if (trustState == MessageTrustState.VERIFIED_TRUSTED) {
                        alerts[i] = alerts[i].copy(isSafe = true, trustState = trustState.name)
                    }
                    found = true
                }
            }
            if (!found && trustState == MessageTrustState.VERIFIED_TRUSTED) {
                alerts.add(0, SosAlertRecord(
                    messageId = message.messageId,
                    incidentId = message.incidentId,
                    senderId = message.senderId,
                    messageText = message.messageText,
                    lat = message.lat,
                    lng = message.lng,
                    receivedAt = System.currentTimeMillis(),
                    isSafe = true,
                    trustState = trustState.name
                ))
            }
        } else {
            if (existingIndex >= 0) {
                val existing = alerts[existingIndex]
                if (existing.trustState == MessageTrustState.VERIFIED_TRUSTED.name && trustState != MessageTrustState.VERIFIED_TRUSTED) {
                    return
                }
                alerts[existingIndex] = existing.copy(
                    messageText = message.messageText,
                    lat = message.lat ?: existing.lat,
                    lng = message.lng ?: existing.lng,
                    trustState = trustState.name
                )
            } else {
                alerts.add(0, SosAlertRecord(
                    messageId = message.messageId,
                    incidentId = message.incidentId,
                    senderId = message.senderId,
                    messageText = message.messageText,
                    lat = message.lat,
                    lng = message.lng,
                    receivedAt = System.currentTimeMillis(),
                    isSafe = false,
                    trustState = trustState.name
                ))
            }
        }
        persist(alerts)
    }

    fun getAlert(incidentId: String): SosAlertRecord? {
        return loadAlerts().firstOrNull { it.incidentId == incidentId }
    }

    private fun persist(alerts: List<SosAlertRecord>) {
        val arr = JSONArray()
        alerts.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("sos_alerts", arr.toString()).apply()
    }
}
