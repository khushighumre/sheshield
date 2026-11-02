package com.example.sheshield.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.sheshield.data.ContactStore

@RequiresApi(Build.VERSION_CODES.N)
class SosTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = "SheShield SOS"
        tile.contentDescription = "Trigger Emergency SOS"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (ContactStore(this).load().isEmpty()) {
            Toast.makeText(this, "Add at least one trusted contact first.", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "SheShield SOS Countdown Started!", Toast.LENGTH_SHORT).show()
        SosService.start(this, SosService.ACTION_TRIGGER)
    }
}
