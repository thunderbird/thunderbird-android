package com.fsck.k9.service;

import java.util.ArrayList;
import java.util.Collection;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationActionService extends CoreService {
    private final static String REPLY_ACTION = "com.fsck.k9.service.NotificationActionService.REPLY_ACTION";
    private final static String READ_ALL_ACTION = "com.fsck.k9.service.NotificationActionService.READ_ALL_ACTION";

    private final static String EXTRA_ACCOUNT = "account";
    private final static String EXTRA_MESSAGE = "message";
    private final static String EXTRA_MESSAGE_IDS = "message_ids";

    public static PendingIntent getReplyIntent(Context context, final Account account, final Message message) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE, message.makeMessageReference());
        i.setAction(REPLY_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getReadAllMessagesIntent(Context context, final Account account, final Collection<Message> messages) {
        ArrayList<Long> messageIds = new ArrayList<Long>();

        for (Message m : messages) {
            messageIds.add(m.getId());
        }
        
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_IDS, messageIds);
        i.setAction(READ_ALL_ACTION);
        
        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int startService(Intent intent, int startId) {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "NotificationActionService started with startId = " + startId);
        final Preferences preferences = Preferences.getPreferences(this);
        final MessagingController controller = MessagingController.getInstance(getApplication());
        final Account account = preferences.getAccount(intent.getStringExtra(EXTRA_ACCOUNT));

        if (account != null) {
            if (READ_ALL_ACTION.equals(intent.getAction())) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService marking messages as read");

                ArrayList<Long> messageIds = (ArrayList<Long>) intent.getSerializableExtra(EXTRA_MESSAGE_IDS);

                controller.setFlag(account, messageIds, Flag.SEEN, true, false);
            } else if (REPLY_ACTION.equals(intent.getAction())) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService initiating reply");

                try {
                    MessageReference ref = (MessageReference) intent.getParcelableExtra(EXTRA_MESSAGE);
                    Folder folder = account.getLocalStore().getFolder(ref.folderName);
                    if (folder != null) {
                        Message message = folder.getMessage(ref.uid);
                        if (message != null) {
                            Intent i = MessageCompose.getActionReplyIntent(this, account, message, false, null);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                        } else {
                            Log.w(K9.LOG_TAG, "Could not find message for notification action.");
                        }
                    } else {
                        Log.w(K9.LOG_TAG, "Could not find folder for notification action.");
                    }
                } catch (MessagingException e) {
                    Log.w(K9.LOG_TAG, "Could not execute reply action.", e);
                }
            }

            /* there's no point in keeping the notification after the user clicked on it */
            controller.notifyAccountCancel(this, account);
        } else {
            Log.w(K9.LOG_TAG, "Could not find account for notification action.");
        }
        
        return START_NOT_STICKY;
    }
}
