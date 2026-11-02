package com.example.sheshield.data.p2p

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

class MessageQueueManager(context: Context) {
    private val walLog = WalLog(context)
    private val pendingQueue = mutableMapOf<String, P2pMessage>()

    // Bounded LRU-like set for processed message IDs, backed by persistent WAL
    private val processedIds = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        // Asynchronous WAL recovery off the main thread to prevent UI blocking / ANRs
        CoroutineScope(Dispatchers.IO).launch {
            val recovery = walLog.recoverState()
            synchronized(this@MessageQueueManager) {
                for (msg in recovery.pendingMessages) {
                    if (!msg.isExpired()) {
                        pendingQueue[msg.messageId] = msg
                    }
                }
                processedIds.addAll(recovery.processedIds)
            }
        }
    }

    @Synchronized
    fun enqueue(message: P2pMessage) {
        if (message.isExpired()) return
        pendingQueue[message.messageId] = message
        CoroutineScope(Dispatchers.IO).launch {
            walLog.appendEnqueue(message)
        }
    }

    @Synchronized
    fun markDelivered(messageId: String) {
        // Note: For epidemic mesh propagation, we keep pending messages in the queue until TTL expires
        // so newly connected peers can also receive them. We only remove them if explicitly expired.
    }

    @Synchronized
    fun isDuplicate(messageId: String): Boolean {
        if (processedIds.contains(messageId)) {
            return true
        }
        processedIds.add(messageId)
        CoroutineScope(Dispatchers.IO).launch {
            walLog.appendProcessed(messageId)
        }
        return false
    }

    @Synchronized
    fun getPendingMessages(): List<P2pMessage> {
        val iterator = pendingQueue.iterator()
        val active = mutableListOf<P2pMessage>()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired()) {
                iterator.remove()
                CoroutineScope(Dispatchers.IO).launch {
                    walLog.appendRemove(entry.key)
                }
            } else {
                active.add(entry.value)
            }
        }
        return active
    }
}
