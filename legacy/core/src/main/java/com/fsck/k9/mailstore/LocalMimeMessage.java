package com.fsck.k9.mailstore;


import com.fsck.k9.mail.internet.MimeMessage;
import net.thunderbird.feature.account.AccountId;


public class LocalMimeMessage extends MimeMessage implements LocalPart {
    private final AccountId accountId;
    private final LocalMessage message;
    private final long messagePartId;

    public LocalMimeMessage(AccountId accountId, LocalMessage message, long messagePartId) {
        super();
        this.accountId = accountId;
        this.message = message;
        this.messagePartId = messagePartId;
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
    public LocalMessage getMessage() {
        return message;
    }
}
