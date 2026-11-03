package com.example.sheshield

import com.example.sheshield.data.p2p.HlcTimestamp
import com.example.sheshield.data.p2p.MessageType
import com.example.sheshield.data.p2p.P2pMessage
import org.junit.Assert.*
import org.junit.Test

class P2pSecurityTest {

    @Test
    fun testHlcOrderingAndCausality() {
        val t1 = HlcTimestamp(1000L, 0, "deviceA")
        val t2 = HlcTimestamp(1000L, 1, "deviceA")
        val t3 = HlcTimestamp(2000L, 0, "deviceB")

        assertTrue(t1.compareTo(t2) < 0)
        assertTrue(t2.compareTo(t3) < 0)
        assertTrue(t3.compareTo(t1) > 0)
    }

    @Test
    fun testP2pMessageTtlAndHopCount() {
        val hlc = HlcTimestamp(System.currentTimeMillis() - 400_000L, 0, "deviceA")
        val expiredMsg = P2pMessage(
            incidentId = "inc-1",
            senderId = "deviceA",
            type = MessageType.SOS,
            messageText = "Test SOS",
            hlc = hlc,
            ttl = 300L
        )
        assertTrue(expiredMsg.isExpired())
        assertFalse(expiredMsg.canForward())

        val freshHlc = HlcTimestamp(System.currentTimeMillis(), 0, "deviceA")
        val freshMsg = P2pMessage(
            incidentId = "inc-2",
            senderId = "deviceA",
            type = MessageType.SOS,
            messageText = "Fresh SOS",
            hlc = freshHlc,
            ttl = 300L,
            hopCount = 3,
            maxHops = 4
        )
        assertFalse(freshMsg.isExpired())
        assertTrue(freshMsg.canForward())

        val forwarded = freshMsg.prepareForForwarding()
        assertEquals(4, forwarded.hopCount)
        assertFalse(forwarded.canForward())
    }

    @Test
    fun testCanonicalPayloadSerialization() {
        val hlc = HlcTimestamp(123456789L, 2, "deviceA")
        val msg = P2pMessage(
            messageId = "msg-123",
            incidentId = "inc-456",
            senderId = "deviceA",
            type = MessageType.SOS,
            messageText = "Emergency help",
            lat = 12.345,
            lng = 67.890,
            hlc = hlc,
            ttl = 300L,
            hopCount = 0,
            senderPublicKey = "PUBKEY",
            signature = "SIG"
        )
        val payload = msg.canonicalPayload()
        val payloadStr = String(payload, Charsets.UTF_8)
        assertTrue(payloadStr.contains("msg-123"))
        assertTrue(payloadStr.contains("inc-456"))
        assertTrue(payloadStr.contains("Emergency help"))
        assertEquals("PUBKEY", msg.senderPublicKey)
        assertEquals("SIG", msg.signature)
    }

    @Test
    fun testLegacyUnsignedTrustState() {
        val hlc = HlcTimestamp(System.currentTimeMillis(), 0, "deviceA")
        val msg = P2pMessage(
            messageId = "msg-legacy",
            incidentId = "inc-legacy",
            senderId = "deviceA",
            type = MessageType.SOS,
            messageText = "Legacy unsigned SOS",
            hlc = hlc,
            signature = "",
            senderPublicKey = ""
        )
        assertTrue(msg.signature.isBlank())
        assertTrue(msg.senderPublicKey.isBlank())
    }
}
