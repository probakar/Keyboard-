package com.customboard.keyboard.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.customboard.keyboard.R
import com.customboard.keyboard.databinding.ActivityThemePickerBinding
import com.customboard.keyboard.keyboard.KeyboardRenderer
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.theme.ThemePresets
import com.customboard.keyboard.utils.dpToPx

/** Visual theme gallery: every preset is drawn as a miniature keyboard. */
class ThemePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemePickerBinding
    private val renderer by lazy { KeyboardRenderer(this) }
    private val themeManager by lazy { ThemeManager.getInstance(this) }
    private val prefs by lazy { PreferencesManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.settings_theme)

        binding.themeGrid.layoutManager = GridLayoutManager(this, 2)
        binding.themeGrid.adapter = ThemeAdapter(availableThemes())
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
