package com.example.sheshield.data.p2p

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.Scanner

class WalLog(context: Context) {
    private val logFile = File(context.filesDir, "sheshield_p2p_wal.log")

    companion object {
        private const val TAG = "SheShieldWal"
    }

    @Synchronized
    fun appendEnqueue(message: P2pMessage) {
        try {
            val entry = JSONObject().apply {
                put("action", "ENQUEUE")
                put("message", message.toJson())
            }
            FileWriter(logFile, true).use { writer ->
                writer.append(entry.toString()).append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append ENQUEUE to WAL", e)
        }
    }

    @Synchronized
    fun appendRemove(messageId: String) {
        try {
            val entry = JSONObject().apply {
                put("action", "REMOVE")
                put("messageId", messageId)
            }
            FileWriter(logFile, true).use { writer ->
                writer.append(entry.toString()).append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append REMOVE to WAL", e)
        }
    }

    @Synchronized
    fun appendProcessed(messageId: String) {
        try {
            val entry = JSONObject().apply {
                put("action", "PROCESSED")
                put("messageId", messageId)
            }
            FileWriter(logFile, true).use { writer ->
                writer.append(entry.toString()).append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append PROCESSED to WAL", e)
        }
    }

    data class RecoveryResult(
        val pendingMessages: List<P2pMessage>,
        val processedIds: Set<String>
    )

    @Synchronized
    fun recoverState(): RecoveryResult {
        val pending = mutableMapOf<String, P2pMessage>()
        val processed = mutableSetOf<String>()
        if (!logFile.exists()) return RecoveryResult(emptyList(), emptySet())

        try {
            Scanner(logFile).use { scanner ->
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine().trim()
                    if (line.isEmpty()) continue
                    try {
                        val json = JSONObject(line)
                        when (json.getString("action")) {
                            "ENQUEUE" -> {
                                val msg = P2pMessage.fromJson(json.getJSONObject("message"))
                                if (!msg.isExpired()) {
                                    pending[msg.messageId] = msg
                                }
                            }
                            "REMOVE" -> {
                                val id = json.getString("messageId")
                                pending.remove(id)
                            }
                            "PROCESSED" -> {
                                val id = json.getString("messageId")
                                processed.add(id)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping corrupted WAL line: $line", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WAL log", e)
        }

        // Compact log after recovery
        compact(pending.values.toList(), processed)

        return RecoveryResult(pending.values.toList(), processed)
    }

    @Synchronized
    fun compact(activeMessages: List<P2pMessage>, processedIds: Set<String>) {
        try {
            val tempFile = File(logFile.parent, "sheshield_p2p_wal_temp.log")
            FileWriter(tempFile, false).use { writer ->
                for (msg in activeMessages) {
                    val entry = JSONObject().apply {
                        put("action", "ENQUEUE")
                        put("message", msg.toJson())
                    }
                    writer.append(entry.toString()).append("\n")
                }
                for (id in processedIds.toList().takeLast(200)) {
                    val entry = JSONObject().apply {
                        put("action", "PROCESSED")
                        put("messageId", id)
                    }
                    writer.append(entry.toString()).append("\n")
                }
            }
            if (logFile.exists()) {
                logFile.delete()
            }
            tempFile.renameTo(logFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compact WAL", e)
        }
    }
}
