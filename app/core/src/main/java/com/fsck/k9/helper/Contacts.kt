package com.fsck.k9.helper

import android.net.Uri
import app.k9mail.core.android.common.contact.ContactDataSource
import app.k9mail.core.common.mail.EmailAddress
import com.fsck.k9.mail.Address
import timber.log.Timber

/**
 * Helper class to access the contacts stored on the device.
 */
open class Contacts(
    private val contactDataSource: ContactDataSource,
) {

    /**
     * Check whether the provided email address belongs to one of the contacts.
     *
     * @param emailAddress The email address to look for.
     * @return <tt>true</tt>, if the email address belongs to a contact.
     * <tt>false</tt>, otherwise.
     */
    fun isInContacts(emailAddress: EmailAddress): Boolean = contactDataSource.hasContactFor(emailAddress)

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
        val contact = contactDataSource.getContactFor(emailAddress)
        return contact?.uri
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

        val contact = contactDataSource.getContactFor(emailAddress)
        return if (contact != null) {
            nameCache[emailAddress] = contact.name
            contact.name
        } else {
            null
        }
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
            val contact = contactDataSource.getContactFor(emailAddress)
            contact?.photoUri
        } catch (e: Exception) {
            Timber.e(e, "Couldn't fetch photo for contact with email ${emailAddress.address}")
            null
        }
    }

    companion object {
        private val nameCache = HashMap<EmailAddress, String?>()

        /**
         * Clears the cache for names and photo uris
         */
        fun clearCache() {
            nameCache.clear()
        }
    }
}
