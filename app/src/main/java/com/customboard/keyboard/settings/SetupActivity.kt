package com.customboard.keyboard.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.customboard.keyboard.R
import com.customboard.keyboard.databinding.ActivitySetupBinding
import com.customboard.keyboard.utils.KeyboardUtils
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.visible

/**
 * First-run wizard: enable the keyboard, select it, grant the optional permissions and pick a
 * theme. Every step checks the real system state, so the user can never get stuck.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private val prefs by lazy { PreferencesManager.getInstance(this) }

    private var step = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.primaryButton.setOnClickListener { onPrimaryAction() }
        binding.secondaryButton.setOnClickListener { advance() }
        binding.finishButton.setOnClickListener { finishSetup() }
        render()
    }

    override fun onResume() {
        super.onResume()
        syncStep()
        render()
    }

    private fun syncStep() {
        step = when {
            !KeyboardUtils.isImeEnabled(this) -> 0
            !KeyboardUtils.isImeSelected(this) -> 1
            step < 2 -> 2
            else -> step
        }
    }

    private fun onPrimaryAction() {
        when (step) {
            0 -> KeyboardUtils.openImeSettings(this)
            1 -> KeyboardUtils.showImePicker(this)
            2 -> startActivity(
                PermissionRequestActivity.intentFor(this, android.Manifest.permission.RECORD_AUDIO)
            )

            3 -> startActivity(Intent(this, ThemePickerActivity::class.java))
            else -> finishSetup()
        }
    }

    private fun advance() {
        step = (step + 1).coerceAtMost(4)
        render()
    }

    private fun render() {
        val total = 5
        binding.stepIndicator.text = getString(R.string.setup_step, step + 1, total)
        binding.progress.max = total
        binding.progress.progress = step + 1

        when (step) {
            0 -> bind(
                R.string.setup_enable_title, R.string.setup_enable_body,
                R.string.setup_enable_action, R.drawable.ic_keyboard
            )

            1 -> bind(
                R.string.setup_select_title, R.string.setup_select_body,
                R.string.setup_select_action, R.drawable.ic_check_circle
            )

            2 -> bind(
                R.string.setup_permissions_title, R.string.setup_permissions_body,
                R.string.setup_permissions_action, R.drawable.ic_mic
            )

            3 -> bind(
                R.string.setup_theme_title, R.string.setup_theme_body,
                R.string.setup_theme_action, R.drawable.ic_palette
            )

            else -> bind(
                R.string.setup_done_title, R.string.setup_done_body,
                R.string.setup_done_action, R.drawable.ic_check_circle
            )
        }

        val lastStep = step >= 4
        binding.finishButton.visibility = if (lastStep) android.view.View.VISIBLE else android.view.View.GONE
        binding.primaryButton.visibility = if (lastStep) android.view.View.GONE else android.view.View.VISIBLE
        if (step in 2..3) binding.secondaryButton.visible() else binding.secondaryButton.gone()
    }

    private fun bind(titleRes: Int, bodyRes: Int, actionRes: Int, iconRes: Int) {
        binding.title.setText(titleRes)
        binding.body.setText(bodyRes)
        binding.primaryButton.setText(actionRes)
        binding.icon.setImageResource(iconRes)
        binding.secondaryButton.setText(R.string.setup_skip)
    }

    private fun finishSetup() {
        prefs.setupComplete = true
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}
