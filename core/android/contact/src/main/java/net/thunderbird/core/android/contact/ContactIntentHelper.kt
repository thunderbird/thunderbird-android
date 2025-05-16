package net.thunderbird.core.android.contact

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.fsck.k9.mail.Address

object ContactIntentHelper {
    @JvmStatic
    fun getContactPickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI)
    }

    /**
     * Get Intent to add information to an existing contact or add a new one.
     *
     * @param address An {@link Address} instance containing the email address
     *              of the entity you want to add to the contacts. Optionally
     *              the instance also contains the (display) name of that
     *              entity.
     */
    fun getAddEmailContactIntent(address: Address): Intent {
        return Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.fromParts("mailto", address.address, null)
            putExtra(ContactsContract.Intents.EXTRA_CREATE_DESCRIPTION, address.toString())

            if (address.personal != null) {
                putExtra(ContactsContract.Intents.Insert.NAME, address.personal)
            }
        }
    }

    /**
     * Get Intent to add a phone number to an existing contact or add a new one.
     *
     * @param phoneNumber
     *         The phone number to add to a contact, or to use when creating a new contact.
     */
    fun getAddPhoneContactIntent(phoneNumber: String): Intent {
        return Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, Uri.decode(phoneNumber))
        }
    }
}
