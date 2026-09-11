package com.fsck.k9.ui.messagedetails

import android.content.res.Resources
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.fsck.k9.helper.ContactNameProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager

/**
 * Get the display name for a participant to be shown in the message details screen.
 */
internal interface MessageDetailsParticipantFormatter {
    fun getDisplayName(address: Address, account: LegacyAccountDto, isSender: Boolean): CharSequence?
}

internal class RealMessageDetailsParticipantFormatter(
    private val contactNameProvider: ContactNameProvider,
    private val showContactNames: Boolean,
    private val contactNameColor: Int?,
    private val toMeText: String,
    private val fromMeText: String,
) : MessageDetailsParticipantFormatter {
    override fun getDisplayName(address: Address, account: LegacyAccountDto, isSender: Boolean): CharSequence? {
        val identity = account.findIdentity(address)
        if (identity != null) {
            return getIdentityName(identity, account, isSender)
        }

        return if (showContactNames) {
            getContactNameOrNull(address) ?: address.personal
        } else {
            address.personal
        }
    }

    private fun getIdentityName(identity: Identity, account: LegacyAccountDto, isSender: Boolean): String {
        val meText = if (isSender) fromMeText else toMeText
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
    messageListPreferencesManager: MessageListPreferencesManager,
): MessageDetailsParticipantFormatter {
    val messageListPreferences = messageListPreferencesManager.getConfig()
    return RealMessageDetailsParticipantFormatter(
        contactNameProvider = contactNameProvider,
        showContactNames = messageListPreferences.isShowContactName,
        contactNameColor = if (
            messageListPreferences.isChangeContactNameColor
        ) {
            messageListPreferences.contactNameColor
        } else {
            null
        },
        toMeText = resources.getString(R.string.message_view_to_me_text),
        fromMeText = resources.getString(R.string.message_view_from_me_text),
    )
}
