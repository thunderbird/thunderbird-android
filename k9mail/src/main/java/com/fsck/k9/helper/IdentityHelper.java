package com.fsck.k9.helper;


import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;

public class IdentityHelper {

    /**
     * Find the identity a message was sent to.
     *
     * @param account
     *         The account the message belongs to.
     * @param message
     *         The message to get the recipients from.
     *
     * @return The identity the message was sent to, or the account's default identity if it
     *         couldn't be determined which identity this message was sent to.
     *
     * @see Account#findIdentity(com.fsck.k9.mail.Address)
     */
    public static Identity getRecipientIdentityFromMessage(Account account, Message message) {
        Identity recipient = null;

        for (Address address : message.getRecipients(Message.RecipientType.TO)) {
            Identity identity = account.findIdentity(address);
            if (identity != null) {
                recipient = identity;
                break;
            }
        }
        if (recipient == null) {
            Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
            if (ccAddresses.length > 0) {
                for (Address address : ccAddresses) {
                    Identity identity = account.findIdentity(address);
                    if (identity != null) {
                        recipient = identity;
                        break;
                    }
                }
            }
        }

        if (recipient == null) {
            recipient = account.getIdentity(0);
        }

        return recipient;
    }
}
