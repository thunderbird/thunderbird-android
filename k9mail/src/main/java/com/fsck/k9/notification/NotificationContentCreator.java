package com.fsck.k9.notification;


import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.message.extractors.PreviewResult.PreviewType;


class NotificationContentCreator {
    private final Context context;


    public NotificationContentCreator(Context context) {
        this.context = context;
    }

    public NotificationContent createFromMessage(Account account, LocalMessage message) {
        MessageReference messageReference = message.makeMessageReference();
        String sender = getMessageSender(account, message);
        String displaySender = getMessageSenderForDisplay(sender);
        String subject = getMessageSubject(message);
        CharSequence preview = getMessagePreview(message);
        CharSequence summary = buildMessageSummary(sender, subject);
        boolean starred = message.isSet(Flag.FLAGGED);

        return new NotificationContent(messageReference, displaySender, subject, preview, summary, starred);
    }

    private CharSequence getMessagePreview(LocalMessage message) {
        String subject = message.getSubject();
        String snippet = getPreview(message);

        boolean isSubjectEmpty = TextUtils.isEmpty(subject);
        boolean isSnippetPresent = snippet != null;
        if (isSubjectEmpty && isSnippetPresent) {
            return snippet;
        }

        String displaySubject = getMessageSubject(message);

        SpannableStringBuilder preview = new SpannableStringBuilder();
        preview.append(displaySubject);
        if (isSnippetPresent) {
            preview.append('\n');
            preview.append(snippet);
        }
        
        return preview;
    }

    private String getPreview(LocalMessage message) {
        PreviewType previewType = message.getPreviewType();
        switch (previewType) {
            case NONE:
            case ERROR:
                return null;
            case TEXT:
                return message.getPreview();
            case ENCRYPTED:
                return context.getString(R.string.preview_encrypted);
        }

        throw new AssertionError("Unknown preview type: " + previewType);
    }

    private CharSequence buildMessageSummary(String sender, String subject) {
        if (sender == null) {
            return subject;
        }

        SpannableStringBuilder summary = new SpannableStringBuilder();
        summary.append(sender);
        summary.append(" ");
        summary.append(subject);

        return summary;
    }

    private String getMessageSubject(Message message) {
        String subject = message.getSubject();
        if (!TextUtils.isEmpty(subject)) {
            return subject;
        }

        return context.getString(R.string.general_no_subject);
    }

    private String getMessageSender(Account account, Message message) {
        boolean isSelf = false;
        final Contacts contacts = K9.showContactName() ? Contacts.getInstance(context) : null;
        final Address[] fromAddresses = message.getFrom();

        if (fromAddresses != null) {
            isSelf = account.isAnIdentity(fromAddresses);
            if (!isSelf && fromAddresses.length > 0) {
                return MessageHelper.toFriendly(fromAddresses[0], contacts).toString();
            }
        }

        if (isSelf) {
            // show To: if the message was sent from me
            Address[] recipients = message.getRecipients(Message.RecipientType.TO);

            if (recipients != null && recipients.length > 0) {
                return context.getString(R.string.message_to_fmt,
                        MessageHelper.toFriendly(recipients[0], contacts).toString());
            }
        }

        return null;
    }

    private String getMessageSenderForDisplay(String sender) {
        return (sender != null) ? sender : context.getString(R.string.general_no_sender);
    }
}
