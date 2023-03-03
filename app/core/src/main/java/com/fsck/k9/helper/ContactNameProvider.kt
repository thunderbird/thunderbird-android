package com.fsck.k9.helper

import app.k9mail.core.common.mail.EmailAddress

interface ContactNameProvider {
    fun getNameForAddress(address: String): String?
}

class RealContactNameProvider(private val contacts: Contacts) : ContactNameProvider {
    override fun getNameForAddress(address: String): String? {
        return contacts.getNameFor(EmailAddress(address))
    }
}
