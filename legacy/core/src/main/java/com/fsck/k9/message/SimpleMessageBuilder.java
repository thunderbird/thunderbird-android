package com.fsck.k9.message;


import android.content.Intent;
import androidx.annotation.VisibleForTesting;

import com.fsck.k9.CoreResourceProvider;
import app.k9mail.legacy.di.DI;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MessageIdGenerator;
import com.fsck.k9.mail.internet.MimeMessage;
import net.thunderbird.core.preference.GeneralSettingsManager;


public class SimpleMessageBuilder extends MessageBuilder {

    public static SimpleMessageBuilder newInstance() {
        MessageIdGenerator messageIdGenerator = MessageIdGenerator.getInstance();
        BoundaryGenerator boundaryGenerator = BoundaryGenerator.getInstance();
        CoreResourceProvider resourceProvider = DI.get(CoreResourceProvider.class);
        GeneralSettingsManager settingsManager = DI.get(GeneralSettingsManager.class);
        return new SimpleMessageBuilder(messageIdGenerator, boundaryGenerator, resourceProvider, settingsManager);
    }

    @VisibleForTesting
    SimpleMessageBuilder(
        MessageIdGenerator messageIdGenerator,
        BoundaryGenerator boundaryGenerator,
        CoreResourceProvider resourceProvider,
        GeneralSettingsManager settingsManager
        ) {
        super(messageIdGenerator, boundaryGenerator, resourceProvider, settingsManager);
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
