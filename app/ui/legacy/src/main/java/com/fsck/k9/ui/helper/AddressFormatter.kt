package com.fsck.k9.ui.helper

import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.mail.Address

/**
 * Get the display name for an email address.
 */
interface AddressFormatter {
    /**
     * Get the display name for an [Address].
     *
     * Use this method for cases where only this display name is shown to the user. Falls back to the email address if
     * necessary.
     */
    fun getDisplayName(address: Address): CharSequence

    /**
     * Get the display name for an [Address], if available.
     */
    fun getDisplayNameOrNull(address: Address): CharSequence?
}

class RealAddressFormatter(
    private val contactNameProvider: ContactNameProvider,
    private val account: Account,
    private val showCorrespondentNames: Boolean,
    private val showContactNames: Boolean,
    private val contactNameColor: Int?,
    private val meText: String
) : AddressFormatter {
    override fun getDisplayName(address: Address): CharSequence {
        val identity = account.findIdentity(address)
        if (identity != null) {
            return getIdentityName(identity)
        }

        return if (!showCorrespondentNames) {
            address.address
        } else if (showContactNames) {
            getContactNameOrNull(address) ?: buildDisplayName(address)
        } else {
            buildDisplayName(address)
        }
    }

    override fun getDisplayNameOrNull(address: Address): CharSequence? {
        val identityDisplayName = account.findIdentity(address)?.name
        if (identityDisplayName != null) {
            return identityDisplayName
        }

        return if (showContactNames) {
            getContactNameOrNull(address) ?: address.personal
        } else {
            address.personal
        }
    }

    private fun getIdentityName(identity: Identity): String {
        return if (account.identities.size == 1) {
            meText
        } else {
            identity.description ?: identity.name ?: identity.email ?: meText
        }
    }

    private fun getContactNameOrNull(address: Address): CharSequence? {
        val contactName = contactNameProvider.getNameForAddress(address.address) ?: return null

        return if (contactNameColor != null) {
            SpannableString(contactName).apply {
                setSpan(ForegroundColorSpan(contactNameColor), 0, contactName.length, SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            contactName
        }
    }

    private fun buildDisplayName(address: Address): CharSequence {
        return address.personal?.takeIf {
            it.isNotBlank() && !it.equals(meText, ignoreCase = true) && !isSpoofAddress(it)
        } ?: address.address
    }

    private fun isSpoofAddress(displayName: String): Boolean {
        val atIndex = displayName.indexOf('@')
        return if (atIndex > 0) {
            displayName[atIndex - 1] != '('
        } else {
            false
        }
    }
}
