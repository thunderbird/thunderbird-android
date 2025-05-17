package com.fsck.k9.ui.messagedetails

import android.content.res.Resources
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.fsck.k9.K9
import com.fsck.k9.helper.ContactNameProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount

/**
 * Get the display name for a participant to be shown in the message details screen.
 */
internal interface MessageDetailsParticipantFormatter {
    fun getDisplayName(address: Address, account: LegacyAccount): CharSequence?
}

internal class RealMessageDetailsParticipantFormatter(
    private val contactNameProvider: ContactNameProvider,
    private val showContactNames: Boolean,
    private val contactNameColor: Int?,
    private val meText: String,
) : MessageDetailsParticipantFormatter {
    override fun getDisplayName(address: Address, account: LegacyAccount): CharSequence? {
        val identity = account.findIdentity(address)
        if (identity != null) {
            return getIdentityName(identity, account)
        }

        return if (showContactNames) {
            getContactNameOrNull(address) ?: address.personal
        } else {
            address.personal
        }
    }

    private fun getIdentityName(identity: Identity, account: LegacyAccount): String {
        return if (account.identities.size == 1) {
            meText
        } else {
            identity.description ?: identity.name ?: meText
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
    contactNameProvider: ContactNameProvider,
    resources: Resources,
): MessageDetailsParticipantFormatter {
    return RealMessageDetailsParticipantFormatter(
        contactNameProvider = contactNameProvider,
        showContactNames = K9.isShowContactName,
        contactNameColor = if (K9.isChangeContactNameColor) K9.contactNameColor else null,
        meText = resources.getString(R.string.message_view_me_text),
    )
}
