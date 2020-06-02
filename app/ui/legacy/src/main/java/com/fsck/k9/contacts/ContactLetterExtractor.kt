package com.fsck.k9.contacts

import com.fsck.k9.mail.Address
import java.util.Locale

class ContactLetterExtractor {
    fun extractContactLetter(address: Address): String {
        val displayName = address.personal ?: address.address

        val matchResult = EXTRACT_LETTER_PATTERN.find(displayName)
        return matchResult?.value?.toUpperCase(Locale.ROOT) ?: FALLBACK_CONTACT_LETTER
    }

    companion object {
        private val EXTRACT_LETTER_PATTERN = Regex("\\p{L}\\p{M}*")
        private const val FALLBACK_CONTACT_LETTER = "?"
    }
}
