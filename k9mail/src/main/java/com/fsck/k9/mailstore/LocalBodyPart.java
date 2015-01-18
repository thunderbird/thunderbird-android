package com.fsck.k9.mailstore;


import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;


public class LocalBodyPart extends MimeBodyPart implements LocalPart {
    private final String accountUuid;
    private final LocalMessage message;
    private final long messagePartId;
    private final String displayName;
    private final long size;
    private final boolean firstClassAttachment;

    public LocalBodyPart(String accountUuid, LocalMessage message, long messagePartId, String displayName, long size,
            boolean firstClassAttachment) throws MessagingException {
        super();
        this.accountUuid = accountUuid;
        this.message = message;
        this.messagePartId = messagePartId;
        this.displayName = displayName;
        this.size = size;
        this.firstClassAttachment = firstClassAttachment;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public long getId() {
        return messagePartId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean isFirstClassAttachment() {
        return firstClassAttachment;
    }

    @Override
    public LocalMessage getMessage() {
        return message;
    }
}
