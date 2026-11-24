package com.fsck.k9.activity.compose;


import android.os.AsyncTask;
import android.os.Handler;

import net.thunderbird.core.android.account.LegacyAccount;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Message;
import net.thunderbird.feature.account.AccountId;


public class SaveMessageTask extends AsyncTask<Void, Void, Void> {
    private final MessagingController messagingController;
    private final AccountId accountId;
    private final Handler handler;
    private final Message message;
    private final Long existingDraftId;
    private final String plaintextSubject;

    public SaveMessageTask(MessagingController messagingController, AccountId accountId, Handler handler, Message message,
            Long existingDraftId, String plaintextSubject) {
        this.messagingController = messagingController;
        this.accountId = accountId;
        this.handler = handler;
        this.message = message;
        this.existingDraftId = existingDraftId;
        this.plaintextSubject = plaintextSubject;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Long messageId = messagingController.saveDraft(accountId, message, existingDraftId, plaintextSubject);

        android.os.Message msg = android.os.Message.obtain(handler, MessageCompose.MSG_SAVED_DRAFT, messageId);
        handler.sendMessage(msg);

        return null;
    }
}
