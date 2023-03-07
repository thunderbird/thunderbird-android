package app.k9mail.core.android.common.contact

import android.net.Uri
import app.k9mail.core.common.mail.EmailAddress

const val CONTACT_ID = 123L
const val CONTACT_NAME = "user name"
const val CONTACT_LOOKUP_KEY = "0r1-4F314D4F2F294F29"
val CONTACT_EMAIL_ADDRESS = EmailAddress("user@example.com")
val CONTACT_URI: Uri = Uri.parse("content://com.android.contacts/contacts/lookup/$CONTACT_LOOKUP_KEY/$CONTACT_ID")
val CONTACT_PHOTO_URI: Uri = Uri.parse("content://com.android.contacts/display_photo/$CONTACT_ID")

val CONTACT = Contact(
    id = CONTACT_ID,
    name = CONTACT_NAME,
    emailAddress = CONTACT_EMAIL_ADDRESS,
    uri = CONTACT_URI,
    photoUri = CONTACT_PHOTO_URI,
)
