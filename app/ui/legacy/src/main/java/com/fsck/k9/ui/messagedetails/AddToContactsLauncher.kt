package com.fsck.k9.ui.messagedetails

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract

internal class AddToContactsLauncher {
    fun launch(context: Context, name: String?, email: String) {
        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE

            putExtra(ContactsContract.Intents.Insert.EMAIL, email)

            if (name != null) {
                putExtra(ContactsContract.Intents.Insert.NAME, name)
            }

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }

        context.startActivity(intent)
    }
}
