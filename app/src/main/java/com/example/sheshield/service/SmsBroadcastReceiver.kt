package com.example.sheshield.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

class SmsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SMS_SENT = "SMS_SENT_ACTION"
        const val ACTION_SMS_DELIVERED = "SMS_DELIVERED_ACTION"
        private const val TAG = "SheShieldSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SMS_SENT -> {
                when (resultCode) {
                    Activity.RESULT_OK -> Log.i(TAG, "SMS transmission confirmed sent by radio.")
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Log.e(TAG, "SMS failed: Generic failure.")
                    SmsManager.RESULT_ERROR_RADIO_OFF -> Log.e(TAG, "SMS failed: Radio is off (Airplane mode?).")
                    SmsManager.RESULT_ERROR_NULL_PDU -> Log.e(TAG, "SMS failed: Null PDU.")
                    SmsManager.RESULT_ERROR_NO_SERVICE -> Log.e(TAG, "SMS failed: No cellular service.")
                    else -> Log.w(TAG, "SMS sent result code: $resultCode")
                }
            }
            ACTION_SMS_DELIVERED -> {
                when (resultCode) {
                    Activity.RESULT_OK -> Log.i(TAG, "SMS delivery confirmed by recipient carrier.")
                    Activity.RESULT_CANCELED -> Log.w(TAG, "SMS delivery canceled or unconfirmed.")
                    else -> Log.w(TAG, "SMS delivery result code: $resultCode")
                }
            }
        }
    }
}
