package com.android.email;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.android.email.mail.Address;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;

public class IntentBroadcastingMessagingListener
    extends MessagingListener {

    @Override
    public void synchronizeMailboxNewMessage(Context context, Account account, String folder, Message message) {
        try {
            Uri uri = Uri.parse("email://messages/" + account.getAccountNumber() + "/" + Uri.encode(folder) + "/" + Uri.encode(message.getUid()));
            Intent intent = new Intent(Email.Intents.EmailReceived.ACTION_EMAIL_RECEIVED, uri);
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_ACCOUNT, account.getDescription());
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_FOLDER, folder);
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_SENT_DATE, message.getSentDate());
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_FROM, Address.toString(message.getFrom()));
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_TO, Address.toString(message.getRecipients(Message.RecipientType.TO)));
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_CC, Address.toString(message.getRecipients(Message.RecipientType.CC)));
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_BCC, Address.toString(message.getRecipients(Message.RecipientType.BCC)));
            intent.putExtra(Email.Intents.EmailReceived.EXTRA_SUBJECT, message.getSubject());
            context.sendBroadcast(intent);
            Log.d(Email.LOG_TAG, "Broadcasted intent: " + message.getSubject());
        }
        catch (MessagingException e) {
            Log.w(Email.LOG_TAG, "Account=" + account.getName() + " folder=" + folder + "message uid=" + message.getUid(), e);
        }
    }

}//IntentBroadcastingMessagingListener
