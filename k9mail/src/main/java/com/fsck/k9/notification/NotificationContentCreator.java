package com.fsck.k9.notification;


import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;


class NotificationContentCreator {
    private final Context context;
    private TextAppearanceSpan emphasizedSpan;


    public NotificationContentCreator(Context context) {
        this.context = context;
    }

    public NotificationContent createFromMessage(Account account, LocalMessage message) {
        MessageReference messageReference = message.makeMessageReference();
        String sender = getMessageSender(account, message);
        String subject = getMessageSubject(message);
        CharSequence preview = getMessagePreview(message);
        CharSequence summary = buildMessageSummary(sender, subject);
        boolean starred = message.isSet(Flag.FLAGGED);

        return new NotificationContent(messageReference, sender, subject, preview, summary, starred);
    }

    private CharSequence getMessagePreview(Message message) {
        String subject = getMessageSubject(message);
        String snippet = message.getPreview();

        if (TextUtils.isEmpty(subject)) {
            return snippet;
        } else if (TextUtils.isEmpty(snippet)) {
            return subject;
        }

        SpannableStringBuilder preview = new SpannableStringBuilder();
        preview.append(subject);
        preview.append('\n');
        preview.append(snippet);

        preview.setSpan(getEmphasizedSpan(), 0, subject.length(), 0);

        return preview;
    }

    private CharSequence buildMessageSummary(String sender, String subject) {
        if (sender == null) {
            return subject;
        }

        SpannableStringBuilder summary = new SpannableStringBuilder();
        summary.append(sender);
        summary.append(" ");
        summary.append(subject);

        summary.setSpan(getEmphasizedSpan(), 0, sender.length(), 0);

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
        try {
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

                return context.getString(R.string.general_no_sender);
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to get sender information for notification.", e);
        }

        return null;
    }

    private TextAppearanceSpan getEmphasizedSpan() {
        if (emphasizedSpan == null) {
            emphasizedSpan = new TextAppearanceSpan(context,
                    R.style.TextAppearance_StatusBar_EventContent_Emphasized);
        }
        return emphasizedSpan;
    }
}
