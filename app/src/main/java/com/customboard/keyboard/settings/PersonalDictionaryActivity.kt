package com.customboard.keyboard.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.customboard.keyboard.R
import com.customboard.keyboard.autocorrect.PersonalDictionary
import com.customboard.keyboard.databinding.ActivityDictionaryBinding
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.visible

/** Add, review and remove the words the keyboard has learned or the user added. */
class PersonalDictionaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDictionaryBinding
    private val dictionary by lazy { PersonalDictionary(this) }
    private val words = ArrayList<String>()
    private lateinit var adapter: WordAdapter
    private var showingBlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDictionaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.dictionary_title)

        adapter = WordAdapter()
        binding.wordList.layoutManager = LinearLayoutManager(this)
        binding.wordList.adapter = adapter

        binding.addButton.setOnClickListener { showAddDialog() }
        binding.toggleButton.setOnClickListener {
            showingBlocked = !showingBlocked
            binding.toggleButton.setText(
                if (showingBlocked) R.string.dictionary_show_words else R.string.dictionary_show_blocked
            )
            reload()
        }
        reload()
    }

    private fun reload() {
        words.clear()
        words.addAll(if (showingBlocked) dictionary.blocked() else dictionary.words())
        adapter.notifyDataSetChanged()
        if (words.isEmpty()) binding.emptyLabel.visible() else binding.emptyLabel.gone()
    }

    private fun showAddDialog() {
        val input = android.widget.EditText(this).apply {
            setSingleLine()
            hint = getString(R.string.dictionary_add_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dictionary_add)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val word = input.text?.toString()?.trim().orEmpty()
                if (word.isNotEmpty()) {
                    if (showingBlocked) dictionary.block(word) else dictionary.add(word)
                    reload()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class WordAdapter : RecyclerView.Adapter<WordAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_word, parent, false)
            )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val word = words[position]
            holder.title.text = word
            holder.delete.setOnClickListener {
                if (showingBlocked) dictionary.unblock(word) else dictionary.remove(word)
                reload()
            }
        }

        override fun getItemCount(): Int = words.size

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.word_text)
            val delete: ImageView = view.findViewById(R.id.word_delete)
        }
    }
}
