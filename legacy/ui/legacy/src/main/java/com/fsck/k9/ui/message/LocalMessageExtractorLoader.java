package com.fsck.k9.ui.message;


import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.loader.content.AsyncTaskLoader;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageCryptoAnnotations;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.mailstore.MessageViewInfoExtractor;
import net.thunderbird.core.logging.legacy.Log;


public class LocalMessageExtractorLoader extends AsyncTaskLoader<MessageViewInfo> {
    private final MessageViewInfoExtractor messageViewInfoExtractor;


    private final LocalMessage message;
    private MessageViewInfo messageViewInfo;
    @Nullable
    private MessageCryptoAnnotations annotations;

    public LocalMessageExtractorLoader(Context context, LocalMessage message,
            @Nullable MessageCryptoAnnotations annotations, MessageViewInfoExtractor messageViewInfoExtractor) {
        super(context);
        this.message = message;
        this.annotations = annotations;
        this.messageViewInfoExtractor = messageViewInfoExtractor;
    }

    @Override
    protected void onStartLoading() {
        if (messageViewInfo != null) {
            super.deliverResult(messageViewInfo);
        }

        if (takeContentChanged() || messageViewInfo == null) {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(MessageViewInfo messageViewInfo) {
        this.messageViewInfo = messageViewInfo;
        super.deliverResult(messageViewInfo);
    }

    @Override
    @WorkerThread
    public MessageViewInfo loadInBackground() {
        try {
            return messageViewInfoExtractor.extractMessageForView(message, annotations, message.getAccount().isOpenPgpProviderConfigured());
        } catch (Exception e) {
            Log.e(e, "Error while decoding message");
            return null;
        }
    }

    public boolean isCreatedFor(LocalMessage localMessage, MessageCryptoAnnotations messageCryptoAnnotations) {
        return annotations == messageCryptoAnnotations && message.equals(localMessage);
    }
}
