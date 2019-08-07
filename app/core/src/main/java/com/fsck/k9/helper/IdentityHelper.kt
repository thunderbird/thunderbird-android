package com.fsck.k9.helper

import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.mail.Message


object IdentityHelper {

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
     * @see Account.findIdentity
     */
    @JvmStatic
    fun getRecipientIdentityFromMessage(account: Account, message: Message): Identity {
        var recipient: Identity? = null

        for (address in message.getRecipients(Message.RecipientType.TO)) {
            val identity = account.findIdentity(address)
            if (identity != null) {
                recipient = identity
                break
            }
        }

        if (recipient == null) {
            val ccAddresses = message.getRecipients(Message.RecipientType.CC)
            if (ccAddresses.size > 0) {
                for (address in ccAddresses) {
                    val identity = account.findIdentity(address)
                    if (identity != null) {
                        recipient = identity
                        break
                    }
                }
            }
        }

        if (recipient == null) {
            for (address in message.getRecipients(Message.RecipientType.X_ORIGINAL_TO)) {
                val identity = account.findIdentity(address)
                if (identity != null) {
                    recipient = identity
                    break
                }
            }
        }

        if (recipient == null) {
            for (address in message.getRecipients(Message.RecipientType.DELIVERED_TO)) {
                val identity = account.findIdentity(address)
                if (identity != null) {
                    recipient = identity
                    break
                }
            }
        }

        if (recipient == null) {
            for (address in message.getRecipients(Message.RecipientType.X_ENVELOPE_TO)) {
                val identity = account.findIdentity(address)
                if (identity != null) {
                    recipient = identity
                    break
                }
            }
        }

        return recipient ?: account.getIdentity(0)
    }
}
