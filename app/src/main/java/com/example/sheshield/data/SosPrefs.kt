package com.example.sheshield.data

import android.content.Context

class SosPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("sheshield_prefs", Context.MODE_PRIVATE)

    var sensitivity: Float
        get() = prefs.getFloat("shake_sensitivity", 0.5f)
        set(v) = prefs.edit().putFloat("shake_sensitivity", v).apply()

    var standbyP2pEnabled: Boolean
        get() = prefs.getBoolean("standby_p2p_enabled", true)
        set(v) = prefs.edit().putBoolean("standby_p2p_enabled", v).apply()
}
