package com.example.sheshield.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sheshield.MainActivity
import com.example.sheshield.data.Crypto
import com.example.sheshield.data.SosAlertStore
import com.example.sheshield.data.p2p.HlcTimestamp
import com.example.sheshield.data.p2p.MessageQueueManager
import com.example.sheshield.data.p2p.MessageTrustState
import com.example.sheshield.data.p2p.MessageType
import com.example.sheshield.data.p2p.P2pMessage
import com.example.sheshield.data.p2p.PeerTrustStore
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class NearbyManager(private val context: Context) {

    companion object {
        private const val TAG = "SheShieldP2P"
        private const val SERVICE_ID = "com.example.sheshield.NEARBY_SOS"
        private val STRATEGY = Strategy.P2P_CLUSTER
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val connectedEndpoints = mutableSetOf<String>()
    private var isStarted = false
    private val myEndpointName = "SheShield_Device_${(1000..9999).random()}"
    private val queueManager = MessageQueueManager(context)

    // Callbacks for receiving data
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes()
                if (data != null) {
                    try {
                        val jsonStr = String(data, StandardCharsets.UTF_8)
                        val json = JSONObject(jsonStr)
                        val message = P2pMessage.fromJson(json)
                        handleIncomingMessage(endpointId, message)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse incoming P2pMessage payload", e)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    // Callbacks for connection lifecycle
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.i(TAG, "SheShieldP2P: Connection initiated by ${info.endpointName} ($endpointId)")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.i(TAG, "SheShieldP2P: Connected successfully to $endpointId")
                connectedEndpoints.add(endpointId)
                // Flush any pending queued messages to the newly connected peer
                flushQueueToEndpoint(endpointId)
            } else {
                Log.w(TAG, "SheShieldP2P: Connection failed to $endpointId (status: ${result.status.statusCode})")
                connectedEndpoints.remove(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG, "SheShieldP2P: Disconnected from endpoint $endpointId. Re-triggering discovery to recover connection.")
            connectedEndpoints.remove(endpointId)
            startDiscovery()
        }
    }

    // Callbacks for discovering peers
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "SheShieldP2P: Endpoint discovered: ${info.endpointName} ($endpointId)")
            connectionsClient.requestConnection(
                myEndpointName,
                endpointId,
                connectionLifecycleCallback
            ).addOnSuccessListener {
                Log.i(TAG, "SheShieldP2P: Connection request sent successfully to $endpointId")
            }.addOnFailureListener { e ->
                Log.w(TAG, "SheShieldP2P: Failed to request connection to $endpointId", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i(TAG, "SheShieldP2P: Endpoint lost: $endpointId")
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true
        Log.i(TAG, "SheShieldP2P: Starting standby P2P manager...")
        startAdvertising()
        startDiscovery()
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        Log.i(TAG, "SheShieldP2P: Stopping standby P2P manager...")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            myEndpointName,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "SheShieldP2P: Advertising started successfully for service: $SERVICE_ID")
        }.addOnFailureListener { e ->
            Log.e(TAG, "SheShieldP2P: Advertising failed: ${e.message}", e)
        }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "SheShieldP2P: Discovery started successfully for service: $SERVICE_ID")
        }.addOnFailureListener { e ->
            Log.e(TAG, "SheShieldP2P: Discovery failed: ${e.message}", e)
        }
    }

    fun broadcastSos(incidentId: String, msg: String, lat: Double? = null, lng: Double? = null) {
        val hlc = HlcTimestamp.generate(myEndpointName)
        val tempMsg = P2pMessage(
            incidentId = incidentId,
            senderId = myEndpointName,
            type = MessageType.SOS,
            messageText = msg,
            lat = lat,
            lng = lng,
            hlc = hlc
        )
        val sig = Crypto.signData(tempMsg.canonicalPayload())
        val message = tempMsg.copy(
            senderPublicKey = Crypto.getMyPublicKeyBase64(),
            signature = sig
        )
        queueManager.enqueue(message)
        sendToAllConnected(message)
    }

    fun broadcastSafe(incidentId: String) {
        val hlc = HlcTimestamp.generate(myEndpointName)
        val tempMsg = P2pMessage(
            incidentId = incidentId,
            senderId = myEndpointName,
            type = MessageType.SAFE,
            messageText = "I am safe now.",
            hlc = hlc
        )
        val sig = Crypto.signData(tempMsg.canonicalPayload())
        val message = tempMsg.copy(
            senderPublicKey = Crypto.getMyPublicKeyBase64(),
            signature = sig
        )
        queueManager.enqueue(message)
        sendToAllConnected(message)
    }

    private fun sendToAllConnected(message: P2pMessage) {
        if (connectedEndpoints.isEmpty()) return
        val bytes = message.toJson().toString().toByteArray(StandardCharsets.UTF_8)
        connectionsClient.sendPayload(connectedEndpoints.toList(), Payload.fromBytes(bytes))
            .addOnSuccessListener {
                queueManager.markDelivered(message.messageId)
            }
            .addOnFailureListener {
                Log.w(TAG, "Failed to send payload, remaining in queue for retry")
            }
    }

    private fun flushQueueToEndpoint(endpointId: String) {
        val pending = queueManager.getPendingMessages()
        if (pending.isEmpty()) return

        for (message in pending) {
            if (message.canForward()) {
                val bytes = message.toJson().toString().toByteArray(StandardCharsets.UTF_8)
                connectionsClient.sendPayload(listOf(endpointId), Payload.fromBytes(bytes))
            }
        }
    }

    fun evaluateTrust(message: P2pMessage, context: Context): MessageTrustState {
        if (message.messageId.isBlank() || message.incidentId.isBlank() || message.messageText.length > 2000) {
            return MessageTrustState.INVALID
        }
        if (message.signature.isBlank() || message.senderPublicKey.isBlank()) {
            return MessageTrustState.LEGACY_UNSIGNED
        }
        val isValid = Crypto.verifySignature(message.canonicalPayload(), message.signature, message.senderPublicKey)
        if (!isValid) {
            return MessageTrustState.INVALID
        }
        val trustStore = PeerTrustStore(context)
        return if (trustStore.isTrusted(message.senderPublicKey)) {
            MessageTrustState.VERIFIED_TRUSTED
        } else {
            MessageTrustState.SIGNED_UNVERIFIED
        }
    }

    private fun handleIncomingMessage(endpointId: String, message: P2pMessage) {
        val trustState = evaluateTrust(message, context)
        if (trustState == MessageTrustState.INVALID) {
            Log.w(TAG, "SECURITY: Rejected invalid or malformed P2pMessage from ${message.senderId}")
            return
        }

        if (message.isExpired()) {
            Log.i(TAG, "Dropped expired message: ${message.messageId}")
            return
        }

        if (queueManager.isDuplicate(message.messageId)) {
            return
        }

        SosAlertStore(context).saveAlert(message, trustState)
        HlcTimestamp.update(message.hlc, myEndpointName)

        // Only verified trusted messages trigger alerts and auto-forwarding.
        // Unverified or legacy unsigned messages are stored securely but NEVER auto-forwarded or treated as authenticated emergency alerts.
        if (trustState == MessageTrustState.VERIFIED_TRUSTED) {
            showNotification(message)
            if (message.canForward()) {
                val forwardedMessage = message.prepareForForwarding()
                queueManager.enqueue(forwardedMessage)
                val recipients = connectedEndpoints.filter { it != endpointId }
                if (recipients.isNotEmpty()) {
                    val bytes = forwardedMessage.toJson().toString().toByteArray(StandardCharsets.UTF_8)
                    connectionsClient.sendPayload(recipients, Payload.fromBytes(bytes))
                        .addOnSuccessListener {
                            queueManager.markDelivered(forwardedMessage.messageId)
                        }
                }
            }
        } else {
            Log.w(TAG, "SECURITY: Stored unverified message (state: $trustState) from ${message.senderId}. Forwarding suppressed.")
        }
    }

    private fun showNotification(message: P2pMessage) {
        val title = when (message.type) {
            MessageType.SOS -> "🚨 NEARBY SOS ALERT (Hop ${message.hopCount})"
            MessageType.LOCATION_UPDATE -> "📍 NEARBY LOCATION UPDATE"
            MessageType.SAFE -> "🟢 NEARBY SOS RESOLVED"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("incidentId", message.incidentId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, message.messageId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(NotificationManager::class.java)
        val builder = NotificationCompat.Builder(context, "sheshield_sos_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message.messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        nm.notify(message.messageId.hashCode(), builder.build())
    }
}
