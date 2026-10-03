package com.customboard.keyboard.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.customboard.keyboard.R
import com.customboard.keyboard.theme.CustomThemeCreator
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.theme.ThemePresets
import com.customboard.keyboard.theme.ThemeStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Browses community palettes hosted with the project and installs them as editable themes. */
class ThemeStoreActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var progress: View
    private lateinit var adapter: StoreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_store)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.theme_store_title)

        status = findViewById(R.id.store_status)
        progress = findViewById(R.id.store_progress)
        adapter = StoreAdapter(::installTheme)
        findViewById<RecyclerView>(R.id.store_list).apply {
            layoutManager = LinearLayoutManager(this@ThemeStoreActivity)
            adapter = this@ThemeStoreActivity.adapter
        }
        refreshCatalog()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_theme_store, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_theme_store_refresh -> { refreshCatalog(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun refreshCatalog() {
        status.setText(R.string.theme_store_loading)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val catalog = withContext(Dispatchers.IO) {
                ThemeStoreRepository.loadCatalog(applicationContext)
            }
            progress.visibility = View.GONE
            adapter.submit(catalog.themes)
            status.setText(
                when {
                    catalog.themes.isEmpty() -> R.string.theme_store_empty
                    catalog.isOnline -> R.string.theme_store_online
                    else -> R.string.theme_store_offline
                }
            )
        }
    }

    private fun installTheme(theme: ThemeStoreRepository.StoreTheme, button: Button) {
        button.isEnabled = false
        button.setText(R.string.theme_store_installing)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val draft = withContext(Dispatchers.IO) {
                ThemeStoreRepository.loadThemeJson(applicationContext, theme)
                    ?.let(CustomThemeCreator::parseDraft)
            }
            progress.visibility = View.GONE
            if (draft == null) {
                button.isEnabled = true
                button.setText(R.string.theme_install_apply)
                toast(R.string.theme_store_install_failed)
                return@launch
            }
            CustomThemeCreator.save(this@ThemeStoreActivity, draft)
            ThemeManager.getInstance(this@ThemeStoreActivity).applyTheme(ThemePresets.ID_CUSTOM)
            toast(R.string.theme_store_installed)
            finish()
        }
    }

    private fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private inner class StoreAdapter(
        private val onInstall: (ThemeStoreRepository.StoreTheme, Button) -> Unit
    ) : RecyclerView.Adapter<StoreAdapter.Holder>() {

        private val themes = ArrayList<ThemeStoreRepository.StoreTheme>()

        fun submit(values: List<ThemeStoreRepository.StoreTheme>) {
            themes.clear()
            themes.addAll(values)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_theme_store, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val theme = themes[position]
            holder.name.text = theme.name
            holder.description.text = theme.description
            holder.swatch.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(theme.previewColor, darken(theme.previewColor))
            ).apply { cornerRadius = 14f }
            holder.install.setOnClickListener { onInstall(theme, holder.install) }
        }

        override fun getItemCount(): Int = themes.size

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val swatch: View = view.findViewById(R.id.theme_store_swatch)
            val name: TextView = view.findViewById(R.id.theme_store_name)
            val description: TextView = view.findViewById(R.id.theme_store_description)
            val install: Button = view.findViewById(R.id.theme_store_install)
        }
    }

    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * 0.55f).toInt(),
        (Color.green(color) * 0.55f).toInt(),
        (Color.blue(color) * 0.55f).toInt()
    )
}
