package app.k9mail.core.android.common.contact

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import app.k9mail.core.android.common.database.EmptyCursor
import app.k9mail.core.android.common.database.getLongOrThrow
import app.k9mail.core.android.common.database.getStringOrNull
import app.k9mail.core.common.mail.EmailAddress

interface ContactDataSource {

    fun getContactFor(emailAddress: EmailAddress): Contact?

    fun hasContactFor(emailAddress: EmailAddress): Boolean
}

internal class ContentResolverContactDataSource(
    private val contentResolver: ContentResolver,
    private val contactPermissionResolver: ContactPermissionResolver,
) : ContactDataSource {

    override fun getContactFor(emailAddress: EmailAddress): Contact? {
        getCursorFor(emailAddress).use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getLongOrThrow(ContactsContract.CommonDataKinds.Email._ID)
                val lookupKey = cursor.getStringOrNull(ContactsContract.Contacts.LOOKUP_KEY)
                val uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey)

                val name = cursor.getStringOrNull(ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME)

                val photoUri = cursor.getStringOrNull(ContactsContract.CommonDataKinds.Photo.PHOTO_URI)
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
        return if (contactPermissionResolver.hasContactPermission()) {
            val uri = Uri.withAppendedPath(
                ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI,
                Uri.encode(emailAddress.address),
            )

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
