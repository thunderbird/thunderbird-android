package com.fsck.k9.ui.messageview

import android.content.res.Resources
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import app.k9mail.legacy.account.Identity
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.helper.ContactNameProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R

/**
 * Get the display name for a recipient to be shown in the message view screen.
 */
internal interface MessageViewRecipientFormatter {
    fun getDisplayName(address: Address, account: Account): CharSequence
}

internal class RealMessageViewRecipientFormatter(
    private val contactNameProvider: ContactNameProvider,
    private val showCorrespondentNames: Boolean,
    private val showContactNames: Boolean,
    private val contactNameColor: Int?,
    private val meText: String,
) : MessageViewRecipientFormatter {
    override fun getDisplayName(address: Address, account: Account): CharSequence {
        val identity = account.findIdentity(address)
        if (identity != null) {
            return getIdentityName(identity, account)
        }

        return if (!showCorrespondentNames) {
            address.address
        } else if (showContactNames) {
            getContactName(address)
        } else {
            buildDisplayName(address)
        }
    }

    private fun getIdentityName(identity: Identity, account: Account): String {
        return if (account.identities.size == 1) {
            meText
        } else {
            identity.description ?: identity.name ?: identity.email ?: meText
        }
    }

    private fun getContactName(address: Address): CharSequence {
        val contactName = contactNameProvider.getNameForAddress(address.address) ?: return buildDisplayName(address)

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

internal fun createMessageViewRecipientFormatter(
    contactNameProvider: ContactNameProvider,
    resources: Resources,
): MessageViewRecipientFormatter {
    return RealMessageViewRecipientFormatter(
        contactNameProvider = contactNameProvider,
        showCorrespondentNames = K9.isShowCorrespondentNames,
        showContactNames = K9.isShowContactName,
        contactNameColor = if (K9.isChangeContactNameColor) K9.contactNameColor else null,
        meText = resources.getString(R.string.message_view_me_text),
    )
}
