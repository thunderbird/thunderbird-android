package com.fsck.k9.ui.message;


import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;

import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;


public class LocalMessageLoader extends AsyncTaskLoader<LocalMessage> {
    private final MessagingController controller;
    private final Account account;
    private final MessageReference messageReference;
    private final boolean onlyLoadMetadata;
    private LocalMessage message;

    public LocalMessageLoader(Context context, MessagingController controller, Account account,
            MessageReference messageReference, boolean onlyLoadMetaData) {
        super(context);
        this.controller = controller;
        this.account = account;
        this.messageReference = messageReference;
        this.onlyLoadMetadata = onlyLoadMetaData;
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
            if (onlyLoadMetadata) {
                return loadMessageMetadataFromDatabase();
            } else {
                return loadMessageFromDatabase();
            }
        } catch (Exception e) {
            Timber.e(e, "Error while loading message from database");
            return null;
        }
    }

    private LocalMessage loadMessageMetadataFromDatabase() throws MessagingException {
        return controller.loadMessageMetadata(account, messageReference.getFolderServerId(), messageReference.getUid());
    }

    private LocalMessage loadMessageFromDatabase() throws MessagingException {
        return controller.loadMessage(account, messageReference.getFolderServerId(), messageReference.getUid());
    }

    public boolean isCreatedFor(MessageReference messageReference) {
        return this.messageReference.equals(messageReference);
    }
}
