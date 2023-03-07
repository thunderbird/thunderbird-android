package app.k9mail.core.android.common.contact

import android.net.Uri
import app.k9mail.core.common.mail.EmailAddress

data class Contact(
    val id: Long,
    val name: String?,
    val emailAddress: EmailAddress,
    val uri: Uri,
    val photoUri: Uri?,
)
