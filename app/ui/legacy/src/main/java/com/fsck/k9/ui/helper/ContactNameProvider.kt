package com.fsck.k9.ui.helper

import com.fsck.k9.helper.Contacts

interface ContactNameProvider {
    fun getNameForAddress(address: String): String?
}

class RealContactNameProvider(private val contacts: Contacts) : ContactNameProvider {
    override fun getNameForAddress(address: String): String? {
        return contacts.getNameForAddress(address)
    }
}
