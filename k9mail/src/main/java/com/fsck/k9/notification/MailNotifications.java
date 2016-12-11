package com.fsck.k9.notification;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;

public class MailNotifications extends BaseNotifications {

    public MailNotifications(NotificationController controller, NotificationActionCreator actionCreator) {
        super(controller, actionCreator);
    }

    protected void addMarkAsReadAction(NotificationCompat.Builder builder, NotificationContent content, int notificationId) {
        int icon = getMarkAsReadActionIcon();
        String title = context.getString(R.string.notification_action_mark_as_read);


        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    protected void addDeleteAction(NotificationCompat.Builder builder, NotificationContent content, int notificationId) {
        if (!isDeleteActionEnabled()) {
            return;
        }

        int icon = getDeleteActionIcon();
        String title = context.getString(R.string.notification_action_delete);

        MessageReference messageReference = content.messageReference;
        PendingIntent action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, action);
    }

    protected void addReplyAction(NotificationCompat.Builder builder, NotificationContent content, int notificationId) {
        int icon = getReplyActionIcon();
        String title = context.getString(R.string.notification_action_reply);

        MessageReference messageReference = content.messageReference;
        PendingIntent replyToMessagePendingIntent =
                actionCreator.createReplyPendingIntent(messageReference, notificationId);

        builder.addAction(icon, title, replyToMessagePendingIntent);
    }

    protected int getMarkAsReadActionIcon() {
        return R.drawable.notification_action_mark_as_read;
    }

    protected int getDeleteActionIcon() {
        return R.drawable.notification_action_delete;
    }

    protected int getReplyActionIcon() {
        return R.drawable.notification_action_reply;
    }


    protected boolean isPrivacyModeActive() {
        KeyguardManager keyguardService = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        boolean privacyModeAlwaysEnabled = K9.getNotificationHideSubject() == K9.NotificationHideSubject.ALWAYS;
        boolean privacyModeEnabledWhenLocked = K9.getNotificationHideSubject() == K9.NotificationHideSubject.WHEN_LOCKED;
        boolean screenLocked = keyguardService.inKeyguardRestrictedInputMode();

        return privacyModeAlwaysEnabled || (privacyModeEnabledWhenLocked && screenLocked);
    }
}
