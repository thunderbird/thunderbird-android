package com.fsck.k9.helper

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto

object IdentityHelper {
    private val RECIPIENT_TYPES = listOf(
        RecipientType.TO,
        RecipientType.CC,
        RecipientType.X_ORIGINAL_TO,
        RecipientType.DELIVERED_TO,
        RecipientType.X_ENVELOPE_TO,
    )

    /**
     * Find the identity a message was sent to.
     *
     * @param account
     * The account the message belongs to.
     * @param message
     * The message to get the recipients from.
     *
     * @return The identity the message was sent to, or the account's default identity if it
     * couldn't be determined which identity this message was sent to.
     *
     * @see LegacyAccountDto.findIdentity
     */
    @JvmStatic
    @JvmOverloads
    fun getRecipientIdentityFromMessage(
        account: LegacyAccountDto,
        message: Message,
        allowRecipientAddressForReply: Boolean = false,
    ): Identity {
        val recipient: Identity? = RECIPIENT_TYPES.asSequence()
            .flatMap { recipientType -> message.getRecipients(recipientType).asSequence() }
            .map { address ->
                account.findIdentity(address)
                    ?: createRecipientIdentity(account, address.address, allowRecipientAddressForReply)
            }
            .filterNotNull()
            .firstOrNull()

        return recipient ?: account.getIdentity(0)
    }

    private fun createRecipientIdentity(
        account: LegacyAccountDto,
        recipientAddress: String,
        allowRecipientAddressForReply: Boolean,
    ): Identity? {
        if (!allowRecipientAddressForReply || !account.useRecipientAddressForReply) return null

        val configuredDomain = account.recipientAddressReplyDomain
            .trim()
            .takeIf { it.isValidDomain() }
            ?.lowercase()
        val recipientDomain = recipientAddress.domainOrNull()
        val domainsMatch = configuredDomain != null && recipientDomain?.equals(configuredDomain, ignoreCase = true) == true
        val sourceIdentity = if (domainsMatch) {
            account.identities.firstOrNull { identity ->
                identity.email?.domainOrNull()?.equals(configuredDomain, ignoreCase = true) == true
            } ?: account.getIdentity(0)
        } else {
            null
        }

        return sourceIdentity?.copy(email = recipientAddress)
    }

    private fun String.domainOrNull(): String? {
        val separatorIndex = lastIndexOf('@')
        val domain = substring(separatorIndex + 1)
        return domain.takeIf { candidate ->
            separatorIndex > 0 && indexOf('@') == separatorIndex && candidate.isNotEmpty() &&
                none { character -> character.isWhitespace() }
        }
    }

    private fun String.isValidDomain(): Boolean {
        if (isEmpty() || length > 253 || any { it.isWhitespace() } || contains('@') || contains('/') || contains(':')) {
            return false
        }

        return split('.').size >= 2 && split('.').all { label ->
            label.isNotEmpty() && label.length <= 63 &&
                label.first().isLetterOrDigit() && label.last().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }
}
