package com.fsck.k9.ui.messagedetails

import android.net.Uri
import com.fsck.k9.mail.Address

data class MessageDetailsUi(
    val date: String?,
    val from: List<Participant>,
    val sender: List<Participant>,
    val replyTo: List<Participant>,
    val to: List<Participant>,
    val cc: List<Participant>,
    val bcc: List<Participant>
)

data class Participant(
    val address: Address,
    val contactLookupUri: Uri?
) {
    val isInContacts: Boolean
        get() = contactLookupUri != null
}
