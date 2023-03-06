package com.fsck.k9.helper

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import androidx.core.content.ContextCompat
import app.k9mail.core.android.common.database.EmptyCursor
import com.fsck.k9.mail.Address
import timber.log.Timber

/**
 * Helper class to access the contacts stored on the device.
 */
open class Contacts(
    private var context: Context,
) {
    private var contentResolver: ContentResolver = context.contentResolver

    /**
     * Check whether the provided email address belongs to one of the contacts.
     *
     * @param emailAddress The email address to look for.
     * @return <tt>true</tt>, if the email address belongs to a contact.
     * <tt>false</tt>, otherwise.
     */
    fun isInContacts(emailAddress: String): Boolean {
        var result = false
        val cursor = getContactByAddress(emailAddress)
        if (cursor != null) {
            if (cursor.count > 0) {
                result = true
            }
            cursor.close()
        }
        return result
    }

    /**
     * Check whether one of the provided addresses belongs to one of the contacts.
     *
     * @param addresses The addresses to search in contacts
     * @return <tt>true</tt>, if one address belongs to a contact.
     * <tt>false</tt>, otherwise.
     */
    fun isAnyInContacts(addresses: Array<Address>?): Boolean {
        if (addresses == null) {
            return false
        }
        for (addr in addresses) {
            if (isInContacts(addr.address)) {
                return true
            }
        }
        return false
    }

    fun getContactUri(emailAddress: String): Uri? {
        val cursor = getContactByAddress(emailAddress) ?: return null
        cursor.use {
            if (!cursor.moveToFirst()) {
                return null
            }
            val contactId = cursor.getLong(CONTACT_ID_INDEX)
            val lookupKey = cursor.getString(LOOKUP_KEY_INDEX)
            return ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
        }
    }

    /**
     * Get the name of the contact an email address belongs to.
     *
     * @param address The email address to search for.
     * @return The name of the contact the email address belongs to. Or
     * <tt>null</tt> if there's no matching contact.
     */
    open fun getNameForAddress(address: String?): String? {
        if (address == null) {
            return null
        } else if (nameCache.containsKey(address)) {
            return nameCache[address]
        }
        val cursor = getContactByAddress(address)
        var name: String? = null
        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                name = cursor.getString(NAME_INDEX)
            }
            cursor.close()
        }
        nameCache[address] = name
        return name
    }

    /**
     * Mark contacts with the provided email addresses as contacted.
     */
    fun markAsContacted(addresses: Array<Address?>?) {
        // TODO: Keep track of this information in a local database. Then use this information when sorting contacts for
        // auto-completion.
    }

    /**
     * Get URI to the picture of the contact with the supplied email address.
     *
     * @param address
     * An email address. The contact database is searched for a contact with this email
     * address.
     *
     * @return URI to the picture of the contact with the supplied email address. `null` if
     * no such contact could be found or the contact doesn't have a picture.
     */
    fun getPhotoUri(address: String): Uri? {
        return try {
            val cursor = getContactByAddress(address) ?: return null
            try {
                if (!cursor.moveToFirst()) {
                    return null
                }
                val columnIndex = cursor.getColumnIndex(CommonDataKinds.Photo.PHOTO_URI)
                val uriString = cursor.getString(columnIndex) ?: return null
                Uri.parse(uriString)
            } catch (e: IllegalStateException) {
                null
            } finally {
                cursor.close()
            }
        } catch (e: Exception) {
            Timber.e(e, "Couldn't fetch photo for contact with email $address")
            null
        }
    }

    private fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Return a [Cursor] instance that can be used to fetch information
     * about the contact with the given email address.
     *
     * @param address The email address to search for.
     * @return A [Cursor] instance that can be used to fetch information
     * about the contact with the given email address
     */
    private fun getContactByAddress(address: String): Cursor? {
        val uri = Uri.withAppendedPath(CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(address))
        return if (hasContactPermission()) {
            contentResolver.query(
                uri,
                PROJECTION,
                null,
                null,
                SORT_ORDER,
            )
        } else {
            EmptyCursor()
        }
    }

    companion object {
        /**
         * The order in which the search results are returned by
         * [.getContactByAddress].
         */
        private const val SORT_ORDER = CommonDataKinds.Email.TIMES_CONTACTED + " DESC, " +
            ContactsContract.Contacts.DISPLAY_NAME + ", " +
            CommonDataKinds.Email._ID

        /**
         * Array of columns to load from the database.
         */
        private val PROJECTION = arrayOf(
            CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            CommonDataKinds.Email.CONTACT_ID,
            CommonDataKinds.Photo.PHOTO_URI,
            ContactsContract.Contacts.LOOKUP_KEY,
        )

        /**
         * Index of the name field in the projection. This must match the order in
         * [.PROJECTION].
         */
        private const val NAME_INDEX = 1

        /**
         * Index of the contact id field in the projection. This must match the order in
         * [.PROJECTION].
         */
        private const val CONTACT_ID_INDEX = 2
        private const val LOOKUP_KEY_INDEX = 4

        private val nameCache = HashMap<String, String?>()

        /**
         * Clears the cache for names and photo uris
         */
        fun clearCache() {
            nameCache.clear()
        }
    }
}
