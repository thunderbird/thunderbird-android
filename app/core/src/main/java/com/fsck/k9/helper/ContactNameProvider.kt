package com.fsck.k9.helper

import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.common.mail.EmailAddress

interface ContactNameProvider {
    fun getNameForAddress(address: String): String?
}

class RealContactNameProvider(
    private val contactRepository: ContactRepository,
) : ContactNameProvider {
    override fun getNameForAddress(address: String): String? {
        return contactRepository.getContactFor(EmailAddress(address))?.name
    }
}
