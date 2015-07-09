package com.fsck.k9.notification;


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;


class NotificationContentCreator {
    private static final int MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5;


    private final Context context;
    private TextAppearanceSpan emphasizedSpan;


    public NotificationContentCreator(Context context) {
        this.context = context;
    }

    public String createCommaSeparatedListOfSenders(Account account, List<LocalMessage> messages) {
        // Use a LinkedHashSet so that we preserve ordering (newest to oldest), but still remove duplicates
        Set<CharSequence> senders = new LinkedHashSet<CharSequence>(MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION);
        for (Message message : messages) {
            senders.add(getMessageSender(account, message));
            if (senders.size() == MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION) {
                break;
            }
        }

        return TextUtils.join(", ", senders);
    }

    public CharSequence getMessagePreview(Message message) {
        CharSequence subject = getMessageSubject(message);
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

    public CharSequence buildMessageSummary(CharSequence sender, CharSequence subject) {
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

    public CharSequence getMessageSubject(Message message) {
        String subject = message.getSubject();
        if (!TextUtils.isEmpty(subject)) {
            return subject;
        }

        return context.getString(R.string.general_no_subject);
    }

    public CharSequence getMessageSender(Account account, Message message) {
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
