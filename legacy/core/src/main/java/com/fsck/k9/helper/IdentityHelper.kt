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
    fun getRecipientIdentityFromMessage(account: LegacyAccountDto, message: Message): Identity {
        val recipient: Identity? = RECIPIENT_TYPES.asSequence()
            .flatMap { recipientType -> message.getRecipients(recipientType).asSequence() }
            .map { address -> account.findIdentity(address) }
            .filterNotNull()
            .firstOrNull()

        return recipient ?: account.getIdentity(0)
    }

    /**
     * Find the identity a message was sent from.
     *
     * @param account
     * The account the message belongs to.
     * @param message
     * The message to get the sender from.
     *
     * @return The identity the message was sent from, or `null` if it couldn't be determined which
     * identity sent this message.
     *
     * @see LegacyAccountDto.findIdentity
     */
    @JvmStatic
    fun getSenderIdentityFromMessage(account: LegacyAccountDto, message: Message): Identity? {
        return message.from.firstNotNullOfOrNull { address -> account.findIdentity(address) }
    }
}
