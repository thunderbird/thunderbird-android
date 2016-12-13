package com.fsck.k9.message;


import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Globals;
import com.fsck.k9.Identity;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MessageIdGenerator;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.Date;
import java.util.List;


public class ResentMessageBuilder extends MessageBuilder {
    private final Context context;
    private final MessageIdGenerator messageIdGenerator;

    private MessageReference resentMessageReference;
    private Address[] resentTo;
    private Address[] resentCc;
    private Address[] resentBcc;
    private Date resentDate;
    private boolean resentHideTimeZone;
    private Identity resentIdentity;
    private Body resentBody;

    public static ResentMessageBuilder newInstance() {
        Context context = Globals.getContext();
        MessageIdGenerator messageIdGenerator = MessageIdGenerator.getInstance();
        BoundaryGenerator boundaryGenerator = BoundaryGenerator.getInstance();
        return new ResentMessageBuilder(context, messageIdGenerator, boundaryGenerator);
    }

    public ResentMessageBuilder setResentMessageReference(MessageReference resentMessageReference) {
        this.resentMessageReference = resentMessageReference;
        return this;
    }

    public ResentMessageBuilder setResentTo(List<Address> resentTo) {
        this.resentTo = resentTo.toArray(new Address[resentTo.size()]);
        return this;
    }

    public ResentMessageBuilder setResentCc(List<Address> resentCc) {
        this.resentCc = resentCc.toArray(new Address[resentCc.size()]);
        return this;
    }

    public ResentMessageBuilder setResentBcc(List<Address> resentBcc) {
        this.resentBcc = resentBcc.toArray(new Address[resentBcc.size()]);
        return this;
    }

    public ResentMessageBuilder setResentDate(Date resentDate) {
        this.resentDate = resentDate;
        return this;
    }

    public ResentMessageBuilder setResentHideTimeZone(boolean resentHideTimeZone) {
        this.resentHideTimeZone = resentHideTimeZone;
        return this;
    }

    public ResentMessageBuilder setResentIdentity(Identity resentIdentity) {
        this.resentIdentity = resentIdentity;
        return this;
    }

    public ResentMessageBuilder setResentBody(Body resentBody) {
        this.resentBody = resentBody;
        return this;
    }

    @VisibleForTesting
    ResentMessageBuilder(Context context, MessageIdGenerator messageIdGenerator, BoundaryGenerator boundaryGenerator) {
        super(context, messageIdGenerator, boundaryGenerator);
        this.context = context;
        this.messageIdGenerator = messageIdGenerator;
    }

    @Override
    protected void buildMessageInternal() {
        LocalMessage referenceMessage = resentMessageReference.restoreToLocalMessage(this.context);
        MimeMessage message = referenceMessage.cloneAsSuper(true);

        message.setResentDate(resentDate, resentHideTimeZone);
        message.setRecipients(Message.RecipientType.RESENT_TO, resentTo);
        message.setRecipients(Message.RecipientType.RESENT_CC, resentCc);
        message.setRecipients(Message.RecipientType.RESENT_BCC, resentBcc);
        message.setResentFrom(new Address(resentIdentity.getEmail(), resentIdentity.getName()));
        message.setBody(resentBody);

        String messageId = this.messageIdGenerator.generateMessageId(message);
        message.setResentMessageId(messageId);

        queueMessageBuildSuccess(message);
    }

    @Override
    protected void buildMessageOnActivityResult(int requestCode, Intent data) {
        throw new UnsupportedOperationException();
    }
}
