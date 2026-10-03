package com.customboard.keyboard.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.customboard.keyboard.R
import com.customboard.keyboard.databinding.ActivityThemePickerBinding
import com.customboard.keyboard.keyboard.KeyboardRenderer
import com.customboard.keyboard.theme.BackgroundImageManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.theme.ThemePresets
import com.customboard.keyboard.utils.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Theme gallery with custom editing, an online catalog and local photo backgrounds. */
class ThemePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemePickerBinding
    private val renderer by lazy { KeyboardRenderer(this) }
    private val themeManager by lazy { ThemeManager.getInstance(this) }
    private val prefs by lazy { PreferencesManager.getInstance(this) }

    private val chooseBackground = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        binding.buttonGalleryBackground.isEnabled = false
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                BackgroundImageManager.saveFromUri(this@ThemePickerActivity, uri)
            }
            if (saved) {
                themeManager.invalidate()
                toast(R.string.theme_photo_applied)
            } else {
                toast(R.string.theme_photo_failed)
            }
            updateBackgroundButton()
            binding.buttonGalleryBackground.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.settings_theme)

        binding.themeGrid.layoutManager = GridLayoutManager(this, 2)
        binding.buttonEditTheme.setOnClickListener {
            startActivity(Intent(this, ThemeStudioActivity::class.java))
        }
        binding.buttonThemeStore.setOnClickListener {
            startActivity(Intent(this, ThemeStoreActivity::class.java))
        }
        binding.buttonGalleryBackground.setOnClickListener {
            chooseBackground.launch("image/*")
        }
        binding.buttonClearBackground.setOnClickListener {
            BackgroundImageManager.clear(this)
            themeManager.invalidate()
            updateBackgroundButton()
            toast(R.string.theme_photo_removed)
        }
        showThemes()
        updateBackgroundButton()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) showThemes()
        updateBackgroundButton()
    }

    private fun showThemes() {
        binding.themeGrid.adapter = ThemeAdapter(availableThemes())
    }

    private fun updateBackgroundButton() {
        if (::binding.isInitialized) {
            binding.buttonClearBackground.isEnabled = BackgroundImageManager.hasImage(this)
        }
    }

    /** Every preset, plus Material You on Android 12+ and the user's own theme. */
    private fun availableThemes(): List<ThemeColors> {
        val themes = ArrayList<ThemeColors>(ThemePresets.ALL)
        if (com.customboard.keyboard.utils.DeviceUtils.supportsDynamicColor()) {
            themes.add(
                0,
                com.customboard.keyboard.theme.DynamicColorExtractor.extract(
                    this, com.customboard.keyboard.utils.DeviceUtils.isNightMode(this)
                )
            )
        }
        themes.add(com.customboard.keyboard.theme.CustomThemeCreator.load(this))
        return themes
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private inner class ThemeAdapter(private val themes: List<ThemeColors>) :
        RecyclerView.Adapter<ThemeAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_theme, parent, false)
            )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val theme = themes[position]
            val width = dpToPx(150f).toInt()
            val height = dpToPx(96f).toInt()
            holder.preview.setImageBitmap(renderer.renderPreview(theme, width, height))
            holder.title.text = theme.displayName
            holder.check.visibility =
                if (theme.id == prefs.themeId) View.VISIBLE else View.INVISIBLE
            holder.itemView.setOnClickListener {
                themeManager.applyTheme(theme.id)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = themes.size

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val preview: ImageView = view.findViewById(R.id.theme_preview)
            val title: TextView = view.findViewById(R.id.theme_title)
            val check: ImageView = view.findViewById(R.id.theme_check)
        }
    }
}
