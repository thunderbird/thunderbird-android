package com.fsck.k9.ui.message;


import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;

import net.thunderbird.core.android.account.LegacyAccountDto;
import net.thunderbird.core.logging.legacy.Log;
import app.k9mail.legacy.message.controller.MessageReference;
import com.fsck.k9.controller.MessagingController;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;


public class LocalMessageLoader extends AsyncTaskLoader<LocalMessage> {
    private final MessagingController controller;
    private final LegacyAccountDto account;
    private final MessageReference messageReference;
    private final boolean onlyLoadMetadata;
    private LocalMessage message;

    public LocalMessageLoader(Context context, MessagingController controller, LegacyAccountDto account,
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
            Log.e(e, "Error while loading message from database");
            return null;
        }
    }

    private LocalMessage loadMessageMetadataFromDatabase() throws MessagingException {
        return controller.loadMessageMetadata(account, messageReference.getFolderId(), messageReference.getUid());
    }

    private LocalMessage loadMessageFromDatabase() throws MessagingException {
        return controller.loadMessage(account, messageReference.getFolderId(), messageReference.getUid());
    }

    public boolean isCreatedFor(MessageReference messageReference) {
        return this.messageReference.equals(messageReference);
    }
}
