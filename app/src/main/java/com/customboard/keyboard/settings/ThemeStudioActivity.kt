package com.customboard.keyboard.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.customboard.keyboard.R
import com.customboard.keyboard.databinding.ActivityThemeStudioBinding
import com.customboard.keyboard.keyboard.KeyboardRenderer
import com.customboard.keyboard.theme.CustomThemeCreator
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.theme.ThemePresets
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.widgets.ColorPickerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hands-on colour editor with a live keyboard preview and portable theme JSON import. */
class ThemeStudioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeStudioBinding
    private lateinit var draft: CustomThemeCreator.Draft
    private val renderer by lazy { KeyboardRenderer(this) }
    private val colorRows = mutableMapOf<String, ColorRow>()

    private data class ColorField(
        val key: String,
        val label: Int,
        val read: (CustomThemeCreator.Draft) -> Int,
        val write: (CustomThemeCreator.Draft, Int) -> Unit
    )

    private data class ColorRow(val swatch: View, val field: ColorField)

    private val fields by lazy {
        listOf(
            ColorField("background", R.string.theme_color_background,
                { it.background }, { draft, color -> draft.background = color }),
            ColorField("background_end", R.string.theme_color_gradient_end,
                { it.backgroundEnd }, { draft, color -> draft.backgroundEnd = color }),
            ColorField("key", R.string.theme_color_keys,
                { it.keyBackground }, { draft, color -> draft.keyBackground = color }),
            ColorField("text", R.string.theme_color_text,
                { it.keyText }, { draft, color -> draft.keyText = color }),
            ColorField("accent", R.string.theme_color_accent,
                { it.accent }, { draft, color -> draft.accent = color }),
            ColorField("special", R.string.theme_color_special,
                { it.specialKey }, { draft, color -> draft.specialKey = color })
        )
    }

    private val importThemeFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importTheme(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.theme_customize)

        draft = CustomThemeCreator.loadDraft(this)
        binding.themeName.setText(draft.name)
        binding.gradientSwitch.isChecked = draft.gradient
        binding.gradientSwitch.setOnCheckedChangeListener { _, checked ->
            draft.gradient = checked
            refreshPreview()
        }
        fields.forEach { field -> addColorControl(field) }
        binding.buttonImportTheme.setOnClickListener {
            importThemeFile.launch(arrayOf("application/json", "text/json"))
        }
        binding.buttonReset.setOnClickListener {
            draft = CustomThemeCreator.Draft()
            showDraft()
        }
        binding.buttonSave.setOnClickListener { saveTheme() }
        binding.themeName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                draft.name = s?.toString().orEmpty().take(40)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        refreshPreview()
    }

    private fun addColorControl(field: ColorField) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padding = dpToPx(10f).toInt()
            setPadding(padding, dpToPx(4f).toInt(), padding, dpToPx(4f).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.LTGRAY),
                GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = dpToPx(10f)
                }, null
            )
            isClickable = true
            isFocusable = true
        }
        val label = TextView(this).apply {
            setText(field.label)
            textSize = 15f
            setTextColor(ThemeManager.getInstance(this@ThemeStudioActivity).current.keyText)
        }
        val swatch = View(this).apply {
            contentDescription = getString(field.label)
        }
        row.addView(label, LinearLayout.LayoutParams(0, dpToPx(48f).toInt(), 1f))
        row.addView(swatch, LinearLayout.LayoutParams(dpToPx(38f).toInt(), dpToPx(30f).toInt()))
        row.setOnClickListener { openColorPicker(field) }
        binding.colorControls.addView(
            row,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52f).toInt())
        )
        colorRows[field.key] = ColorRow(swatch, field)
        updateColorRow(field)
    }

    private fun openColorPicker(field: ColorField) {
        val working = draft.copy()
        val picker = ColorPickerView(this).apply {
            color = field.read(working)
            onColorChanged = { color ->
                field.write(working, color)
                renderPreview(working)
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(field.label)
            .setView(picker)
            .setPositiveButton(R.string.action_save) { _, _ ->
                field.write(draft, picker.color)
                updateColorRow(field)
                refreshPreview()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        dialog.setOnDismissListener { refreshPreview() }
        dialog.show()
    }

    private fun updateColorRow(field: ColorField) {
        val swatch = colorRows[field.key]?.swatch ?: return
        swatch.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(8f)
            setColor(field.read(draft))
            setStroke(dpToPx(1f).toInt().coerceAtLeast(1), Color.argb(70, 0, 0, 0))
        }
    }

    private fun showDraft() {
        binding.themeName.setText(draft.name)
        binding.gradientSwitch.isChecked = draft.gradient
        fields.forEach(::updateColorRow)
        refreshPreview()
    }

    private fun refreshPreview() = renderPreview(draft)

    private fun renderPreview(value: CustomThemeCreator.Draft) {
        if (!::binding.isInitialized) return
        val width = (resources.displayMetrics.widthPixels - dpToPx(32f).toInt()).coerceAtLeast(1)
        val height = dpToPx(130f).toInt().coerceAtLeast(1)
        binding.themePreview.setImageBitmap(
            renderer.renderPreview(CustomThemeCreator.buildTheme(value), width, height)
        )
    }

    private fun importTheme(uri: Uri) {
        lifecycleScope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (raw.isNullOrBlank() || raw.length > 64 * 1024) {
                toast(R.string.theme_import_failed)
                return@launch
            }
            val imported = CustomThemeCreator.parseDraft(raw)
            if (imported == null) {
                toast(R.string.theme_import_failed)
            } else {
                draft = imported
                showDraft()
                toast(R.string.theme_imported)
            }
        }
    }

    private fun saveTheme() {
        draft.name = binding.themeName.text?.toString()?.trim().orEmpty().ifBlank { "My theme" }.take(40)
        CustomThemeCreator.save(this, draft)
        ThemeManager.getInstance(this).applyTheme(ThemePresets.ID_CUSTOM)
        toast(R.string.theme_saved)
        finish()
    }

    private fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
