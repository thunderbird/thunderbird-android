package com.fsck.k9.ui.message;


import android.content.AsyncTaskLoader;
import android.content.Context;
import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;


public class LocalMessageLoader extends AsyncTaskLoader<LocalMessage> {
    private final MessagingController controller;
    private final Account account;
    private final MessageReference messageReference;
    private LocalMessage message;

    public LocalMessageLoader(Context context, MessagingController controller, Account account,
            MessageReference messageReference) {
        super(context);
        this.controller = controller;
        this.account = account;
        this.messageReference = messageReference;
    }

    @Override
    protected void onStartLoading() {
        if (message != null) {
            super.deliverResult(message);
        }

        if (takeContentChanged() || message == null) {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(LocalMessage message) {
        this.message = message;
        super.deliverResult(message);
    }

    @Override
    public LocalMessage loadInBackground() {
        try {
            return loadMessageFromDatabase();
        } catch (Exception e) {
            Timber.e(e, "Error while loading message from database");
            return null;
        }
    }

    private LocalMessage loadMessageFromDatabase() throws MessagingException {
        return controller.loadMessage(account, messageReference.getFolderId(), messageReference.getUid());
    }

    public boolean isCreatedFor(MessageReference messageReference) {
        return this.messageReference.equals(messageReference);
    }
}
