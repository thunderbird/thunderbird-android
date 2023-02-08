package com.fsck.k9.ui.messagedetails

import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.helper.ContactNameProvider
import com.fsck.k9.mail.Address

/**
 * Get the display name for a participant to be shown in the message details screen.
 */
internal interface MessageDetailsParticipantFormatter {
    fun getDisplayName(address: Address, account: Account): CharSequence?
}

internal class RealMessageDetailsParticipantFormatter(
    private val contactNameProvider: ContactNameProvider,
    private val showContactNames: Boolean,
    private val contactNameColor: Int?
) : MessageDetailsParticipantFormatter {
    override fun getDisplayName(address: Address, account: Account): CharSequence? {
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
}

internal fun createMessageDetailsParticipantFormatter(
    contactNameProvider: ContactNameProvider
): MessageDetailsParticipantFormatter {
    return RealMessageDetailsParticipantFormatter(
        contactNameProvider = contactNameProvider,
        showContactNames = K9.isShowContactName,
        contactNameColor = if (K9.isChangeContactNameColor) K9.contactNameColor else null
    )
}
