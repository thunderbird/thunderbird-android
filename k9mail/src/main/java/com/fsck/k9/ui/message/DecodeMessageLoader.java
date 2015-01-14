package com.fsck.k9.ui.message;


import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalMessageExtractor;
import com.fsck.k9.mailstore.MessageViewInfo;


public class DecodeMessageLoader extends AsyncTaskLoader<MessageViewInfo> {
    private final Message message;
    private MessageViewInfo messageViewInfo;

    public DecodeMessageLoader(Context context, Message message) {
        super(context);
        this.message = message;
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
    public MessageViewInfo loadInBackground() {
        try {
            return LocalMessageExtractor.decodeMessageForView(getContext(), message);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Error while decoding message", e);
            return null;
        }
    }
}
