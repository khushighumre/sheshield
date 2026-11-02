package com.example.sheshield.widget

import android.app.Activity
import android.os.Bundle
import com.example.sheshield.service.SosService

class WidgetTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start the SOS Service legally from a foreground context
        val action = intent.getStringExtra("SVC_ACTION") ?: SosService.ACTION_TRIGGER
        SosService.start(this, action)
        // Immediately close the invisible activity so the user isn't interrupted
        finish()
    }
}
