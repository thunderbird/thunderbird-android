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
import app.k9mail.core.common.mail.EmailAddress
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
    fun isInContacts(emailAddress: EmailAddress): Boolean {
        var result = false
        val cursor = getContactFor(emailAddress)
        if (cursor != null) {
            if (cursor.count > 0) {
                result = true
            }
            cursor.close()
        }
        return result
    }

    /**
     * Check whether one of the provided email addresses belongs to one of the contacts.
     *
     * @param emailAddresses The email addresses to search in contacts
     * @return <tt>true</tt>, if one of the email addresses belongs to a contact.
     * <tt>false</tt>, otherwise.
     */
    fun isAnyInContacts(emailAddresses: List<EmailAddress>): Boolean =
        emailAddresses.any { emailAddress -> isInContacts(emailAddress) }

    fun getContactUri(emailAddress: EmailAddress): Uri? {
        val cursor = getContactFor(emailAddress) ?: return null
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
     * @param emailAddress The email address to search for.
     * @return The name of the contact the email address belongs to. Or
     * <tt>null</tt> if there's no matching contact.
     */
    open fun getNameFor(emailAddress: EmailAddress): String? {
        if (nameCache.containsKey(emailAddress)) {
            return nameCache[emailAddress]
        }
        val cursor = getContactFor(emailAddress)
        var name: String? = null
        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                name = cursor.getString(NAME_INDEX)
            }
            cursor.close()
        }
        nameCache[emailAddress] = name
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
     * @param emailAddress An email address, the contact database is searched for.
     *
     * @return URI to the picture of the contact with the supplied email address. `null` if
     * no such contact could be found or the contact doesn't have a picture.
     */
    fun getPhotoUri(emailAddress: EmailAddress): Uri? {
        return try {
            val cursor = getContactFor(emailAddress) ?: return null
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
            Timber.e(e, "Couldn't fetch photo for contact with email ${emailAddress.address}")
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
     * @param emailAddress The email address to search for.
     * @return A [Cursor] instance that can be used to fetch information
     * about the contact with the given email address
     */
    private fun getContactFor(emailAddress: EmailAddress): Cursor? {
        val uri = Uri.withAppendedPath(CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress.address))
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
         * [.getContactBy].
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

        private val nameCache = HashMap<EmailAddress, String?>()

        /**
         * Clears the cache for names and photo uris
         */
        fun clearCache() {
            nameCache.clear()
        }
    }
}
