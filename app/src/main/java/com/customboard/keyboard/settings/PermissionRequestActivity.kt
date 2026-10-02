package com.customboard.keyboard.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.customboard.keyboard.R

/**
 * Transparent helper activity: an input method cannot ask for a runtime permission itself,
 * so the keyboard starts this activity when it needs the microphone or the address book.
 */
class PermissionRequestActivity : AppCompatActivity() {

    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            this,
            if (granted) R.string.permission_granted else R.string.permission_denied,
            Toast.LENGTH_SHORT
        ).show()
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permission = intent?.getStringExtra(EXTRA_PERMISSION)
        if (permission.isNullOrBlank()) {
            finish()
            return
        }
        launcher.launch(permission)
    }

    companion object {
        const val EXTRA_PERMISSION = "extra_permission"

        fun intentFor(context: Context, permission: String): Intent =
            Intent(context, PermissionRequestActivity::class.java)
                .putExtra(EXTRA_PERMISSION, permission)
    }
}
