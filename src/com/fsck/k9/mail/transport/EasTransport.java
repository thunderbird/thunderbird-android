
package com.fsck.k9.mail.transport;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.EasStore;

public class EasTransport extends Transport {
    private EasStore store;

    public EasTransport(Account account) throws MessagingException {
        if (account.getRemoteStore() instanceof EasStore) {
            store = (EasStore) account.getRemoteStore();
        } else {
            store = new EasStore(account);
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, ">>> New EasTransport creation complete");
    }

    @Override
    public void open() throws MessagingException {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, ">>> open called on EasTransport ");

        store.getHttpClient();
    }

    @Override
    public void close() {
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        store.sendMessages(new Message[] { message });
    }
}
