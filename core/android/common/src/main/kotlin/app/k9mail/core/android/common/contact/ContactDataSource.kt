package app.k9mail.core.android.common.contact

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import app.k9mail.core.android.common.database.EmptyCursor
import app.k9mail.core.android.common.database.getLongValue
import app.k9mail.core.android.common.database.getStringValue
import app.k9mail.core.common.mail.EmailAddress

interface ContactDataSource {

    fun getContactFor(emailAddress: EmailAddress): Contact?

    fun hasContactFor(emailAddress: EmailAddress): Boolean
}

internal class ContentResolverContactDataSource(
    private val context: Context,
    private val contentResolver: ContentResolver = context.contentResolver,
) : ContactDataSource {

    override fun getContactFor(emailAddress: EmailAddress): Contact? {
        getCursorFor(emailAddress).use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getLongValue(ContactsContract.CommonDataKinds.Email._ID)
                val lookupKey = cursor.getStringValue(ContactsContract.Contacts.LOOKUP_KEY)
                val uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey)

                val name = cursor.getStringValue(ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME)

                val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.Photo.PHOTO_URI)
                    ?.let { photoUriString -> Uri.parse(photoUriString) }

                return Contact(
                    id = contactId,
                    name = name,
                    emailAddress = emailAddress,
                    uri = uri,
                    photoUri = photoUri,
                )
            } else {
                return null
            }
        }
    }

    override fun hasContactFor(emailAddress: EmailAddress): Boolean {
        getCursorFor(emailAddress).use { cursor ->
            return cursor.count > 0
        }
    }

    private fun getCursorFor(emailAddress: EmailAddress): Cursor {
        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI,
            Uri.encode(emailAddress.address),
        )

        return if (hasContactPermission()) {
            contentResolver.query(
                uri,
                PROJECTION,
                null,
                null,
                SORT_ORDER,
            ) ?: EmptyCursor()
        } else {
            EmptyCursor()
        }
    }

    private fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {

        private const val SORT_ORDER = ContactsContract.Contacts.DISPLAY_NAME +
            ", " + ContactsContract.CommonDataKinds.Email._ID

        private val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
            ContactsContract.Contacts.LOOKUP_KEY,
        )
    }
}
