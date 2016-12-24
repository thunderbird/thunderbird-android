package com.fsck.k9.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;

/**
 * Posts notifications that are scheduled for the future by SnoozeController.
 *
 * From https://gist.github.com/BrandonSmith/6679223
 */
public class NotificationPublisher extends BroadcastReceiver {

    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";

    public void onReceive(Context context, Intent intent) {
        MessageReference msg = intent.getParcelableExtra(EXTRA_MESSAGE);

        MessagingController messagingController = MessagingController.getInstance(context);
        messagingController.notifySnoozedMessage(msg);
    }
}