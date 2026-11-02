package com.example.sheshield.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import com.example.sheshield.R
import com.example.sheshield.data.ContactStore
import com.example.sheshield.data.SosPhase
import com.example.sheshield.data.SosState
import com.example.sheshield.service.SosService

class SosWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_SOS = "com.example.sheshield.WIDGET_SOS_CLICK"

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SosWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val intent = Intent(context, SosWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val phase = SosState.phase.value
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_sos)

            val intent = Intent(context, SosWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_SOS
            }

            when (phase) {
                SosPhase.IDLE -> {
                    views.setTextViewText(R.id.widget_text, "🚨 SOS")
                    views.setInt(R.id.widget_button, "setBackgroundResource", R.drawable.widget_bg)
                    intent.putExtra("SVC_ACTION", SosService.ACTION_TRIGGER)
                }
                SosPhase.COUNTDOWN -> {
                    views.setTextViewText(R.id.widget_text, "🟠 SOS\nACTIVATING")
                    views.setInt(R.id.widget_button, "setBackgroundResource", R.drawable.widget_bg_amber)
                    intent.putExtra("SVC_ACTION", SosService.ACTION_CANCEL)
                }
                SosPhase.ACTIVE -> {
                    views.setTextViewText(R.id.widget_text, "🔴 SOS\nACTIVE\n(I'm safe)")
                    views.setInt(R.id.widget_button, "setBackgroundResource", R.drawable.widget_bg_green)
                    intent.putExtra("SVC_ACTION", SosService.ACTION_SAFE)
                }
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                phase.ordinal,
                intent,
                flags
            )

            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_SOS) {
            val svcAction = intent.getStringExtra("SVC_ACTION") ?: SosService.ACTION_TRIGGER

            if (svcAction == SosService.ACTION_TRIGGER) {
                val contacts = ContactStore(context).load()
                if (contacts.isEmpty()) {
                    Toast.makeText(context, "Add at least one trusted contact first.", Toast.LENGTH_LONG).show()
                    return
                }
                Toast.makeText(context, "SheShield SOS Countdown Started!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "SheShield SOS Stopped.", Toast.LENGTH_SHORT).show()
            }

            // App Widgets run in the background, so we must launch a Trampoline Activity
            // to safely start the Foreground Service in Android 12+
            val activityIntent = Intent(context, WidgetTrampolineActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("SVC_ACTION", svcAction)
            }
            context.startActivity(activityIntent)
        }
    }
}
