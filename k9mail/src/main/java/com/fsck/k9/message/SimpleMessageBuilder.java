package com.fsck.k9.message;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;


public class SimpleMessageBuilder extends MessageBuilder {

    public SimpleMessageBuilder(Context context) {
        super(context);
    }

    @Override
    public void buildMessageInternal() {
        try {
            MimeMessage message = build();
            queueMessageBuildSuccess(message);
        } catch (MessagingException me) {
            queueMessageBuildException(me);
        }
    }

    @Override
    protected void buildMessageOnActivityResult(int requestCode, Intent data) {
        throw new UnsupportedOperationException();
    }
}
