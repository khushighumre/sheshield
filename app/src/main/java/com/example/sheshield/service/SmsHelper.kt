package com.example.sheshield.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

object SmsHelper {
    private const val TAG = "SheShieldSms"
    // Keyed by "incidentId:normalizedPhone" to allow separate incidents within 15s while blocking instant spam
    private val lastSentMap = mutableMapOf<String, Pair<String, Long>>()

    fun normalizePhoneNumber(phone: String): String {
        val hasPlus = phone.startsWith("+")
        val digits = phone.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    @Suppress("DEPRECATION")
    fun send(context: Context, phone: String, text: String, incidentId: String? = null) {
        // 1. Permission check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "SMS failed: SEND_SMS permission missing.", Toast.LENGTH_LONG).show()
            }
            return
        }

        // 2. Normalize phone
        val normalizedPhone = normalizePhoneNumber(phone)
        if (normalizedPhone.length < 3) {
            Log.e(TAG, "Invalid phone number: $phone")
            return
        }

        // 3. Incident-scoped deduplication check (prevent identical text to same number within 15s per incident)
        val incidentKey = "${incidentId ?: "global"}:$normalizedPhone"
        val textHash = text.hashCode().toString()
        val now = System.currentTimeMillis()
        val last = lastSentMap[incidentKey]
        if (last != null && last.first == textHash && (now - last.second < 15_000L)) {
            Log.i(TAG, "Duplicate SMS suppressed for $incidentKey")
            return
        }
        lastSentMap[incidentKey] = Pair(textHash, now)

        try {
            // 4. Safe Dual-SIM subscription resolution
            val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    SubscriptionManager.getDefaultSmsSubscriptionId()
                } catch (e: Exception) {
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID
                }
            } else {
                SubscriptionManager.INVALID_SUBSCRIPTION_ID
            }

            val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val baseSms = context.getSystemService(SmsManager::class.java)
                if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && subscriptionId >= 0) {
                    baseSms.createForSubscriptionId(subscriptionId)
                } else {
                    baseSms
                }
            } else {
                if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && subscriptionId >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                } else {
                    SmsManager.getDefault()
                }
            }

            // 5. Setup sent & delivered callbacks using BroadcastReceiver
            val sentIntent = PendingIntent.getBroadcast(
                context, 0, Intent(SmsBroadcastReceiver.ACTION_SMS_SENT),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val deliveredIntent = PendingIntent.getBroadcast(
                context, 0, Intent(SmsBroadcastReceiver.ACTION_SMS_DELIVERED),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val parts = sms.divideMessage(text)
            val sentIntents = ArrayList<PendingIntent>().apply { add(sentIntent) }
            val deliveredIntents = ArrayList<PendingIntent>().apply { add(deliveredIntent) }

            Log.i(TAG, "Submitting SMS to $normalizedPhone (subId: $subscriptionId)...")
            sms.sendMultipartTextMessage(normalizedPhone, null, parts, sentIntents, deliveredIntents)
            Log.i(TAG, "SMS successfully submitted to telephony manager for $normalizedPhone")
        } catch (e: Exception) {
            Log.e(TAG, "SMS submission failed to $normalizedPhone", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "SMS submission failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
