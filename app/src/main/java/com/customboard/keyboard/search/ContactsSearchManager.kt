package com.customboard.keyboard.search

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.customboard.keyboard.settings.PreferencesManager

/**
 * Suggests names, phone numbers and e-mail addresses from the address book.
 * Contacts are only read when the user has granted the permission and enabled the feature,
 * and nothing is ever stored or uploaded.
 */
class ContactsSearchManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    data class Contact(val name: String, val detail: String, val type: Type) {
        enum class Type { NAME, PHONE, EMAIL }
    }

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    val isEnabled: Boolean get() = prefs.suggestContacts && hasPermission()

    /** Names matching [prefix], used for inline suggestions while typing. */
    fun suggestNames(prefix: String, limit: Int = 3): List<String> {
        if (!isEnabled || prefix.length < 2) return emptyList()
        val names = LinkedHashSet<String>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
                arrayOf("$prefix%"),
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC LIMIT 20"
            )?.use { cursor ->
                val index = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                while (cursor.moveToNext() && names.size < limit) {
                    val full = cursor.getString(index)?.trim().orEmpty()
                    if (full.isEmpty()) continue
                    full.split(" ").forEach { part ->
                        if (part.startsWith(prefix, ignoreCase = true) && names.size < limit) {
                            names += part
                        }
                    }
                }
            }
        }
        return names.toList()
    }

    /** Full contact search used by the contacts panel. */
    fun search(query: String, limit: Int = 30): List<Contact> {
        if (!isEnabled) return emptyList()
        val results = ArrayList<Contact>(limit)
        val selection = if (query.isBlank()) null
        else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = if (query.isBlank()) null else arrayOf("%${query.trim()}%")
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                selection,
                args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val number = cursor.getString(numberIndex)?.trim().orEmpty()
                    if (name.isNotEmpty()) {
                        results += Contact(name, number, Contact.Type.PHONE)
                    }
                }
            }
        }
        return results.distinctBy { it.name + it.detail }
    }

    fun emails(query: String, limit: Int = 20): List<Contact> {
        if (!isEnabled) return emptyList()
        val results = ArrayList<Contact>(limit)
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                if (query.isBlank()) null else "${ContactsContract.CommonDataKinds.Email.DISPLAY_NAME} LIKE ?",
                if (query.isBlank()) null else arrayOf("%${query.trim()}%"),
                "${ContactsContract.CommonDataKinds.Email.DISPLAY_NAME} ASC LIMIT $limit"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
                val addressIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val address = cursor.getString(addressIndex)?.trim().orEmpty()
                    if (address.isNotEmpty()) {
                        results += Contact(name.ifEmpty { address }, address, Contact.Type.EMAIL)
                    }
                }
            }
        }
        return results.distinctBy { it.detail }
    }
}
