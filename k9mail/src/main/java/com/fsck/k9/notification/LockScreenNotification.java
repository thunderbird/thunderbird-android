package com.fsck.k9.notification;


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;


class LockScreenNotification {
    private static final int MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5;


    private final Context context;
    private final NotificationController controller;


    LockScreenNotification(NotificationController controller) {
        context = controller.getContext();
        this.controller = controller;
    }

    public static LockScreenNotification newInstance(NotificationController controller) {
        return new LockScreenNotification(controller);
    }

    public void configureLockScreenNotification(Builder builder, NotificationsHolder notificationsHolder) {
        if (!NotificationController.platformSupportsLockScreenNotifications()) {
            return;
        }

        switch (K9.getLockScreenNotificationVisibility()) {
            case NOTHING: {
                builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
                break;
            }
            case APP_NAME: {
                builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                break;
            }
            case EVERYTHING: {
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                break;
            }
            case SENDERS: {
                Notification publicNotification = createPublicNotificationWithSenderList(notificationsHolder);
                builder.setPublicVersion(publicNotification);
                break;
            }
            case MESSAGE_COUNT: {
                Notification publicNotification = createPublicNotificationWithNewMessagesCount(notificationsHolder);
                builder.setPublicVersion(publicNotification);
                break;
            }
        }
    }

    private Notification createPublicNotificationWithSenderList(NotificationsHolder notificationsHolder) {
        Builder builder = createPublicNotification(notificationsHolder);
        int newMessages = notificationsHolder.getNewMessagesCount();
        if (newMessages == 1) {
            NotificationHolder holder = notificationsHolder.getHolderForLatestNotification();
            builder.setContentText(holder.content.sender);
        } else {
            List<NotificationContent> contents = notificationsHolder.getContentForSummaryNotification();
            String senderList = createCommaSeparatedListOfSenders(contents);
            builder.setContentText(senderList);
        }

        return builder.build();
    }

    private Notification createPublicNotificationWithNewMessagesCount(NotificationsHolder notificationsHolder) {
        Builder builder = createPublicNotification(notificationsHolder);
        Account account = notificationsHolder.getAccount();
        String accountName = controller.getAccountName(account);
        builder.setContentText(accountName);

        return builder.build();
    }

    private Builder createPublicNotification(NotificationsHolder notificationsHolder) {
        Account account = notificationsHolder.getAccount();
        int newMessages = notificationsHolder.getNewMessagesCount();
        int unreadCount = notificationsHolder.getUnreadMessageCount();
        String title = context.getResources().getQuantityString(R.plurals.notification_new_messages_title,
                newMessages, newMessages);

        return new Builder(context)
                .setSmallIcon(R.drawable.ic_notify_new_mail_vector)
                .setColor(account.getChipColor())
                .setNumber(unreadCount)
                .setContentTitle(title);
    }


    private String createCommaSeparatedListOfSenders(List<NotificationContent> contents) {
        // Use a LinkedHashSet so that we preserve ordering (newest to oldest), but still remove duplicates
        Set<CharSequence> senders = new LinkedHashSet<CharSequence>(MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION);
        for (NotificationContent content : contents) {
            senders.add(content.sender);
            if (senders.size() == MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION) {
                break;
            }
        }

        return TextUtils.join(", ", senders);
    }
}
