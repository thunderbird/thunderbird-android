package com.fsck.k9.mailstore;


import androidx.annotation.Nullable;
import com.fsck.k9.mail.internet.MimeMessage;
import net.thunderbird.feature.account.AccountId;
import net.thunderbird.feature.account.AccountIdFactory;


public class LocalMimeMessage extends MimeMessage implements LocalPart {
    private final AccountId accountId;
    private final LocalMessage message;
    private final long messagePartId;

    public LocalMimeMessage(@Nullable String accountUuid, LocalMessage message, long messagePartId) {
        super();
        if (accountUuid != null) {
            this.accountId = AccountIdFactory.INSTANCE.of(accountUuid);
        } else {
            this.accountId = null;
        }
        this.message = message;
        this.messagePartId = messagePartId;
    }

    @Nullable
    @Override
    public AccountId getAccountId() {
        return accountId;
    }

    @Nullable
    @Override
    public String getAccountUuid() {
        return accountId == null ? null : accountId.toString();
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
