package com.fsck.k9.activity.compose;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Message;

public class SaveMessageTask extends AsyncTask<Void, Void, Void> {
    Context context;
    Account account;
    Contacts contacts;
    Handler handler;
    Message message;
    long draftId;
    boolean saveRemotely;

    public SaveMessageTask(Context context, Account account, Contacts contacts,
                           Handler handler, Message message, long draftId, boolean saveRemotely) {
        this.context = context;
        this.account = account;
        this.contacts = contacts;
        this.handler = handler;
        this.message = message;
        this.draftId = draftId;
        this.saveRemotely = saveRemotely;
    }

    @Override
    protected Void doInBackground(Void... params) {
        final MessagingController messagingController = MessagingController.getInstance(context);
        Message draftMessage = messagingController.saveDraft(account, message, draftId, saveRemotely);
        draftId = messagingController.getId(draftMessage);

        android.os.Message msg = android.os.Message.obtain(handler, MessageCompose.MSG_SAVED_DRAFT, draftId);
        handler.sendMessage(msg);
        return null;
    }
}
