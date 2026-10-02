package com.customboard.keyboard.privacy

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.customboard.keyboard.R
import com.customboard.keyboard.settings.PreferencesManager

/**
 * Quick Settings tile that turns incognito mode on and off without opening the keyboard.
 * While incognito is on nothing is learned, no clipboard entry is stored and no AI request
 * is sent automatically.
 */
class IncognitoTileService : TileService() {

    private val prefs by lazy { PreferencesManager.getInstance(this) }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        prefs.incognito = !prefs.incognito
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val enabled = prefs.incognito
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.incognito_mode)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_incognito)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (enabled) R.string.state_on else R.string.state_off
            )
        }
        tile.updateTile()
    }
}
