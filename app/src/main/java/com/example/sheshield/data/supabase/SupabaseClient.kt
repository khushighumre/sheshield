package com.example.sheshield.data.supabase

import android.content.Context
import com.example.sheshield.BuildConfig

object SupabaseClient {
    const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    const val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    fun getAuthToken(context: Context): String? {
        val prefs = context.getSharedPreferences("sheshield_supabase", Context.MODE_PRIVATE)
        return prefs.getString("auth_token", null)
    }

    fun setAuthToken(context: Context, token: String?) {
        val prefs = context.getSharedPreferences("sheshield_supabase", Context.MODE_PRIVATE)
        prefs.edit().putString("auth_token", token).apply()
    }
}
