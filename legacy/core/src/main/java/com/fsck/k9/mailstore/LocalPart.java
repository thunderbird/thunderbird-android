package com.fsck.k9.mailstore;


import net.thunderbird.feature.account.AccountId;


public interface LocalPart {
    AccountId getAccountId();
    long getPartId();
    long getSize();
    LocalMessage getMessage();
}
