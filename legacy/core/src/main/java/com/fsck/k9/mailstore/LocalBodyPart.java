package com.fsck.k9.mailstore;


import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import net.thunderbird.feature.account.AccountId;


public class LocalBodyPart extends MimeBodyPart implements LocalPart {
    private final AccountId accountId;
    private final LocalMessage message;
    private final long messagePartId;
    private final long size;

    public LocalBodyPart(AccountId accountId, LocalMessage message, long messagePartId, long size)
            throws MessagingException {
        super();
        this.accountId = accountId;
        this.message = message;
        this.messagePartId = messagePartId;
        this.size = size;
    }

    @Override
    public AccountId getAccountId() {
        return accountId;
    }

    @Override
    public long getPartId() {
        return messagePartId;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public LocalMessage getMessage() {
        return message;
    }
}
