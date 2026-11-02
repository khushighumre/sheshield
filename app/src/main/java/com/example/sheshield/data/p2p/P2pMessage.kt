package com.example.sheshield.data.p2p

import org.json.JSONObject
import java.util.UUID

enum class MessageType {
    SOS, LOCATION_UPDATE, SAFE
}

data class P2pMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val incidentId: String,
    val senderId: String,
    val type: MessageType,
    val messageText: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val hlc: HlcTimestamp,
    val ttl: Long = 300L, // 5 minutes default TTL
    val hopCount: Int = 0,
    val maxHops: Int = 4,
    val senderPublicKey: String = "",
    val signature: String = ""
) {
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return now > (hlc.wallTime + (ttl * 1000L))
    }

    fun canForward(): Boolean {
        return hopCount < maxHops && !isExpired()
    }

    fun prepareForForwarding(): P2pMessage {
        return copy(hopCount = hopCount + 1)
    }

    fun canonicalPayload(): ByteArray {
        val str = "$messageId:$incidentId:$senderId:${type.name}:$messageText:${lat ?: ""}:${lng ?: ""}:${hlc.wallTime}:${hlc.counter}:${hlc.senderId}:$ttl"
        return str.toByteArray(Charsets.UTF_8)
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("messageId", messageId)
        json.put("incidentId", incidentId)
        json.put("senderId", senderId)
        json.put("type", type.name)
        json.put("messageText", messageText)
        if (lat != null) json.put("lat", lat)
        if (lng != null) json.put("lng", lng)
        json.put("hlc", hlc.toJson())
        json.put("ttl", ttl)
        json.put("hopCount", hopCount)
        json.put("maxHops", maxHops)
        json.put("senderPublicKey", senderPublicKey)
        json.put("signature", signature)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): P2pMessage {
            return P2pMessage(
                messageId = json.getString("messageId"),
                incidentId = json.getString("incidentId"),
                senderId = json.getString("senderId"),
                type = MessageType.valueOf(json.getString("type")),
                messageText = json.getString("messageText"),
                lat = if (json.has("lat") && !json.isNull("lat")) json.getDouble("lat") else null,
                lng = if (json.has("lng") && !json.isNull("lng")) json.getDouble("lng") else null,
                hlc = HlcTimestamp.fromJson(json.getJSONObject("hlc")),
                ttl = json.getLong("ttl"),
                hopCount = json.getInt("hopCount"),
                maxHops = json.optInt("maxHops", 4),
                senderPublicKey = json.optString("senderPublicKey", ""),
                signature = json.optString("signature", "")
            )
        }
    }
}
