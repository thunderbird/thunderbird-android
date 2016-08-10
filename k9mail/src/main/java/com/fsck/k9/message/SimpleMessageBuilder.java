package com.fsck.k9.message;


import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Globals;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.UuidGenerator;
import com.fsck.k9.mail.internet.MimeMessage;


public class SimpleMessageBuilder extends MessageBuilder {

    public static SimpleMessageBuilder createSimpleMessageBuilder() {
        Context context = Globals.getContext();
        UuidGenerator uuidGenerator = UuidGenerator.getInstance();
        BoundaryGenerator boundaryGenerator = BoundaryGenerator.getInstance();
        return new SimpleMessageBuilder(context, uuidGenerator, boundaryGenerator);
    }

    @VisibleForTesting
    SimpleMessageBuilder(Context context, UuidGenerator uuidGenerator, BoundaryGenerator boundaryGenerator) {
        super(context, uuidGenerator, boundaryGenerator);
    }

    @Override
    protected void buildMessageInternal() {
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
