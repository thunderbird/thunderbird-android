package com.fsck.k9.ui.identity

import com.fsck.k9.Identity

class IdentityFormatter {
    fun getDisplayName(identity: Identity): String {
        return identity.description ?: getEmailDisplayName(identity)
    }

    fun getEmailDisplayName(identity: Identity): String {
        val senderDisplayName = identity.name
        val emailAddress = identity.email ?: "Invalid"

        return if (senderDisplayName != null) {
            "$senderDisplayName <$emailAddress>"
        } else {
            emailAddress
        }
    }
}
