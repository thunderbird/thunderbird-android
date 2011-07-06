package com.fsck.k9.mail.store.exchange.adapter;

import java.io.IOException;
import java.io.InputStream;

import com.fsck.k9.Account;

public class AccountSyncAdapter extends AbstractSyncAdapter {

    public AccountSyncAdapter(Account account) {
        super(null, account);
     }

    @Override
    public void cleanup() {
    }

    @Override
    public String getCollectionName() {
        return null;
    }

    @Override
    public boolean parse(InputStream is) throws IOException {
        return false;
    }

    @Override
    public boolean sendLocalChanges(Serializer s) throws IOException {
        return false;
    }

    @Override
    public boolean isSyncable() {
        return true;
    }
}
