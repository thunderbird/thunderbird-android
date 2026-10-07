package com.fsck.k9.ui.message;


import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.loader.content.AsyncTaskLoader;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageCryptoAnnotations;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.mailstore.MessageViewInfoExtractor;
import net.thunderbird.core.android.account.LegacyAccount;
import net.thunderbird.core.android.account.LegacyAccountManager;
import net.thunderbird.legacy.logging.Log;


public class LocalMessageExtractorLoader extends AsyncTaskLoader<MessageViewInfo> {
    private final MessageViewInfoExtractor messageViewInfoExtractor;

    private LegacyAccountManager accountManager;

    private final LocalMessage message;
    private MessageViewInfo messageViewInfo;
    @Nullable
    private MessageCryptoAnnotations annotations;

    public LocalMessageExtractorLoader(
        Context context,
        LegacyAccountManager accountManager,
        LocalMessage message,
        @Nullable MessageCryptoAnnotations annotations,
        MessageViewInfoExtractor messageViewInfoExtractor
    ) {
        super(context);
        this.accountManager = accountManager;
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
            LegacyAccount account = accountManager.findById(message.getAccountId());
            boolean isOpenPgpProviderConfigured = account != null && account.isOpenPgpProviderConfigured();
            return messageViewInfoExtractor.extractMessageForView(message, annotations, isOpenPgpProviderConfigured);
        } catch (Exception e) {
            Log.e(e, "Error while decoding message");
            return null;
        }
    }

    public boolean isCreatedFor(LocalMessage localMessage, MessageCryptoAnnotations messageCryptoAnnotations) {
        return annotations == messageCryptoAnnotations && message.equals(localMessage);
    }
}
