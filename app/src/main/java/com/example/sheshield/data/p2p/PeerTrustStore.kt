package com.example.sheshield.data.p2p

import android.content.Context

class PeerTrustStore(context: Context) {
    private val prefs = context.getSharedPreferences("sheshield_peer_trust", Context.MODE_PRIVATE)

    fun isTrusted(publicKey: String): Boolean {
        if (publicKey.isBlank()) return false
        return prefs.getBoolean(publicKey, false)
    }

    fun trustPeer(publicKey: String) {
        if (publicKey.isNotBlank()) {
            prefs.edit().putBoolean(publicKey, true).apply()
        }
    }

    fun untrustPeer(publicKey: String) {
        if (publicKey.isNotBlank()) {
            prefs.edit().remove(publicKey).apply()
        }
    }
}
