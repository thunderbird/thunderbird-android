package com.fsck.k9.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.SnoozeController;

import java.util.concurrent.TimeUnit;

/**
 * Posts notifications that are scheduled for the future by SnoozeController.
 *
 * From https://gist.github.com/BrandonSmith/6679223
 */
public class NotificationPublisher extends BroadcastReceiver {

    private static final String TAG = "NotificationPublisher";
    private static final boolean DEBUG = false;

    public static final String ACTION_OPEN_MESSAGE = "com.fsck.k9.notification.ACTION_OPEN_MESSAGE";
    public static final String ACTION_SNOOZE_AGAIN = "com.fsck.k9.notification.ACTION_SNOOZE_AGAIN";

    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";
    private static final String EXTRA_MESSAGE_MARSHALLED = "EXTRA_MESSAGE_MARSHALLED";
    public static final String EXTRA_SNOOZE_DURATION_IN_MS = "EXTRA_SNOOZE_DURATION_IN_MS";

    /***
     * Holds the notification ID of the originating notification holding this PendingIntent action.
     * This is needed because broadcast pending intents don't automatically cancel the notification
     * for you.
     *
     * See http://stackoverflow.com/questions/18261969/clicking-android-notification-actions-does-not-close-notification-drawer/21783203
     */
    public static final String EXTRA_ORIGIN_NOTIFICATION_ID = "EXTRA_ORIGIN_NOTIFICATION_ID";

    public void onReceive(Context context, Intent intent) {
        MessageReference msg = getMessageRefFromIntent(intent);

        if (DEBUG) {
            Log.d(TAG, "got snoozed event: " + intent);
        }

        switch (intent.getAction()) {
            case ACTION_SNOOZE_AGAIN:
                long duration = intent.getLongExtra(EXTRA_SNOOZE_DURATION_IN_MS, TimeUnit.HOURS.toMillis(1));
                SnoozeController snoozeController = SnoozeController.getInstance(context);
                snoozeController.snoozeMessage(msg, System.currentTimeMillis() + duration);

                manuallyCancelNotificationSinceAndroidWontIfItsABroadcast(context, intent);
                break;

            case ACTION_OPEN_MESSAGE:
                MessagingController messagingController = MessagingController.getInstance(context);
                messagingController.notifySnoozedMessage(msg);
                break;
        }
    }

    private void manuallyCancelNotificationSinceAndroidWontIfItsABroadcast(Context context, Intent intent) {

        int notificationId = intent.getIntExtra(EXTRA_ORIGIN_NOTIFICATION_ID, 0);
        if (notificationId != 0) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    /***
     * This will add a MessageReference to a PendingIntent used in an notification action.
     *
     * We need to manually parcel it and unparcel it with #getMessageRefFromIntent(), because
     * there is a bug in Android that crashes if we try to just add a MessageReference directly
     * as a parcelableExtra.
     *
     * See http://blog.naboo.space/blog/2013/09/01/parcelable-in-pendingintent/
     *
     * @param intent
     * @param messageReference
     */
    public static void safelyAddMessageRefToIntent(Intent intent, MessageReference messageReference) {
        Parcel parcel = Parcel.obtain();
        messageReference.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        intent.putExtra(EXTRA_MESSAGE_MARSHALLED, parcel.marshall());

        parcel.recycle();
    }

    public static MessageReference getMessageRefFromIntent(Intent intent) {
        MessageReference msg = intent.getParcelableExtra(EXTRA_MESSAGE);

        if (msg == null) {
            byte[] byteArrayExtra = intent.getByteArrayExtra(EXTRA_MESSAGE_MARSHALLED);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(byteArrayExtra, 0, byteArrayExtra.length);
            parcel.setDataPosition(0);

            msg = MessageReference.CREATOR.createFromParcel(parcel);

            parcel.recycle();
        }

        return msg;
    }
}