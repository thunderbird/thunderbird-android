package com.fsck.k9.mailstore;


import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;


public class LocalBodyPart extends MimeBodyPart implements LocalPart {
    private final String accountUuid;
    private final long messagePartId;

    public LocalBodyPart(String accountUuid, long messagePartId) throws MessagingException {
        super();
        this.accountUuid = accountUuid;
        this.messagePartId = messagePartId;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public long getId() {
        return messagePartId;
    }
}
