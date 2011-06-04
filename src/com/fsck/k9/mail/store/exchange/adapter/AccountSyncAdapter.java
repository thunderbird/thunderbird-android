package com.fsck.k9.mail.store.exchange.adapter;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

public class AccountSyncAdapter extends AbstractSyncAdapter {

    public AccountSyncAdapter(MailboxAdapter mailbox, AccountAdapter account) {
        super(mailbox, account);
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
