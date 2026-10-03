package com.customboard.keyboard.settings

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.customboard.keyboard.R
import com.customboard.keyboard.databinding.ActivityToolbarCustomizeBinding
import com.customboard.keyboard.toolbar.ToolbarItem
import com.customboard.keyboard.toolbar.ToolbarManager

/** Drag to reorder, switch to hide: full control over the keyboard toolbar. */
class ToolbarCustomizeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolbarCustomizeBinding
    private val manager by lazy { ToolbarManager(this) }
    private val entries = ArrayList<Entry>()
    private lateinit var adapter: ToolbarAdapter

    private data class Entry(val item: ToolbarItem, var enabled: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolbarCustomizeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.toolbar_customize)

        loadEntries()
        adapter = ToolbarAdapter()
        binding.toolbarList.layoutManager = LinearLayoutManager(this)
        binding.toolbarList.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from in entries.indices && to in entries.indices) {
                    entries.add(to, entries.removeAt(from))
                    adapter.notifyItemMoved(from, to)
                    save()
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                viewHolder.itemView.alpha = if (isCurrentlyActive) 0.85f else 1f
            }
        })
        touchHelper.attachToRecyclerView(binding.toolbarList)
        adapter.touchHelper = touchHelper

        binding.resetButton.setOnClickListener {
            manager.resetToDefault()
            loadEntries()
            adapter.notifyDataSetChanged()
        }
    }

    private fun loadEntries() {
        entries.clear()
        val visible = manager.visibleItems()
        visible.forEach { entries += Entry(it, true) }
        manager.hiddenItems().forEach { entries += Entry(it, false) }
    }

    private fun save() {
        manager.setOrder(entries.filter { it.enabled }.map { it.item.id })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class ToolbarAdapter : RecyclerView.Adapter<ToolbarAdapter.Holder>() {

        var touchHelper: ItemTouchHelper? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_toolbar_entry, parent, false)
            )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = entries[position]
            holder.icon.setImageResource(entry.item.iconRes)
            holder.title.setText(entry.item.titleRes)
            holder.toggle.setOnCheckedChangeListener(null)
            holder.toggle.isChecked = entry.enabled
            holder.toggle.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                entry.enabled = checked
                save()
            }
            holder.handle.setOnTouchListener { view, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(holder)
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                false
            }
        }

        override fun getItemCount(): Int = entries.size

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.entry_icon)
            val title: TextView = view.findViewById(R.id.entry_title)
            val toggle: SwitchCompat = view.findViewById(R.id.entry_switch)
            val handle: ImageView = view.findViewById(R.id.entry_handle)
        }
    }
}
