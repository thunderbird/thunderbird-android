package com.fsck.k9.mailstore;


import androidx.annotation.NonNull;
import net.thunderbird.feature.account.AccountId;


public interface LocalPart {
    @NonNull
    AccountId getAccountId();
    long getPartId();
    long getSize();
    LocalMessage getMessage();
}
