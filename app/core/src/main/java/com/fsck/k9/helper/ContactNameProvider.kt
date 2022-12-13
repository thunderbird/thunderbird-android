package com.fsck.k9.helper

interface ContactNameProvider {
    fun getNameForAddress(address: String): String?
}

class RealContactNameProvider(private val contacts: Contacts) : ContactNameProvider {
    override fun getNameForAddress(address: String): String? {
        return contacts.getNameForAddress(address)
    }
}
