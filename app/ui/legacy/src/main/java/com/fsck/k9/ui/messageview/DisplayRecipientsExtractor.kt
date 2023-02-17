package com.fsck.k9.ui.messageview

import com.fsck.k9.Account
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message

/**
 * Extract recipient names from a message to display them in the message view.
 *
 * This class extracts up to [maxNumberOfDisplayRecipients] recipients from the message and converts them to their
 * display name using an [MessageViewRecipientFormatter].
 */
internal class DisplayRecipientsExtractor(
    private val recipientFormatter: MessageViewRecipientFormatter,
    private val maxNumberOfDisplayRecipients: Int,
) {
    fun extractDisplayRecipients(message: Message, account: Account): DisplayRecipients {
        val toRecipients = message.getRecipients(Message.RecipientType.TO)
        val ccRecipients = message.getRecipients(Message.RecipientType.CC)
        val bccRecipients = message.getRecipients(Message.RecipientType.BCC)

        val numberOfRecipients = toRecipients.size + ccRecipients.size + bccRecipients.size

        val identity = sequenceOf(toRecipients, ccRecipients, bccRecipients)
            .flatMap { addressArray -> addressArray.asSequence() }
            .mapNotNull { address -> account.findIdentity(address) }
            .firstOrNull()

        val identityEmail = identity?.email
        val maxAdditionalRecipients = if (identity != null) {
            maxNumberOfDisplayRecipients - 1
        } else {
            maxNumberOfDisplayRecipients
        }

        val recipientNames = sequenceOf(toRecipients, ccRecipients, bccRecipients)
            .flatMap { addressArray -> addressArray.asSequence() }
            .filter { address -> address.address != identityEmail }
            .map { address -> recipientFormatter.getDisplayName(address, account) }
            .take(maxAdditionalRecipients)
            .toList()

        return if (identity != null) {
            val identityAddress = Address(identity.email)
            val meName = recipientFormatter.getDisplayName(identityAddress, account)
            val recipients = listOf(meName) + recipientNames

            DisplayRecipients(recipients, numberOfRecipients)
        } else {
            DisplayRecipients(recipientNames, numberOfRecipients)
        }
    }
}

internal data class DisplayRecipients(
    val recipientNames: List<CharSequence>,
    val numberOfRecipients: Int,
)
