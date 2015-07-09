package com.fsck.k9.notification;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.WearableExtender;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.NotificationDeleteConfirmation;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalMessage;

import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_BLINK_SLOW;


class NewMailNotifications {
    private static final String NOTIFICATION_GROUP_KEY = "newMailNotifications";


    private final Context context;
    private final NotificationController controller;
    private final NotificationActionCreator actionCreator;
    private final NotificationContentCreator contentCreator;
    private final ConcurrentMap<Integer, NotificationsHolder> notificationsHolders =
            new ConcurrentHashMap<Integer, NotificationsHolder>();


    public NewMailNotifications(NotificationController controller, NotificationActionCreator actionCreator) {
        this.controller = controller;
        this.actionCreator = actionCreator;
        this.context = controller.getContext();
        this.contentCreator = new NotificationContentCreator(context);
    }

    public void addNewMailNotification(Account account, LocalMessage message, int previousUnreadMessageCount) {
        NotificationsHolder notificationsHolder = getOrCreateNotificationData(account, previousUnreadMessageCount);
        synchronized (notificationsHolder) {
            notifyAccountWithDataLocked(account, message, notificationsHolder);
        }
    }

    public void removeNewMailNotification(Account account, LocalMessage localMessage) {
        NotificationsHolder notificationsHolder = getNotificationData(account);
        if (notificationsHolder == null) {
            return;
        }

        synchronized (notificationsHolder) {
            MessageReference messageReference = localMessage.makeMessageReference();
            if (notificationsHolder.removeMatchingMessage(context, messageReference)) {
                Integer childNotification = notificationsHolder.getStackedChildNotification(messageReference);
                if (childNotification != null) {
                    getNotificationManager().cancel(childNotification);
                }

                notifyAccountWithDataLocked(account, null, notificationsHolder);
            }
        }
    }

    public void clearNewMailNotifications(Account account) {
        int newMailNotificationId = NotificationIds.getNewMailNotificationId(account);
        getNotificationManager().cancel(newMailNotificationId);

        notificationsHolders.remove(account.getAccountNumber());

        //TODO: clear stacked notifications
    }

    private NotificationsHolder getOrCreateNotificationData(Account account, int previousUnreadMessageCount) {
        int accountNumber = account.getAccountNumber();

        NotificationsHolder notificationsHolder = notificationsHolders.get(accountNumber);
        if (notificationsHolder == null) {
            NotificationsHolder newNotificationHolder = new NotificationsHolder(previousUnreadMessageCount);
            NotificationsHolder oldNotificationHolder =
                    notificationsHolders.putIfAbsent(accountNumber, newNotificationHolder);
            if (oldNotificationHolder != null) {
                notificationsHolder = oldNotificationHolder;
            } else {
                notificationsHolder = newNotificationHolder;
            }
        }

        return notificationsHolder;
    }

    private NotificationsHolder getNotificationData(Account account) {
        int accountNumber = account.getAccountNumber();
        return notificationsHolders.get(accountNumber);
    }

    private void notifyAccountWithDataLocked(Account account, LocalMessage newMessage,
            NotificationsHolder notificationsHolder) {

        boolean updateSilently = false;

        if (newMessage == null) {
            newMessage = notificationsHolder.getNewestMessage(context);
            updateSilently = true;
            if (newMessage == null) {
                clearNewMailNotifications(account);
                return;
            }
        } else {
            notificationsHolder.addMessage(newMessage);
        }

        int newMessagesCount = notificationsHolder.getNewMessagesCount();
        int unreadMessagesCount = notificationsHolder.getUnreadMessagesCountBeforeNotification() + newMessagesCount;
        CharSequence sender = contentCreator.getMessageSender(account, newMessage);
        CharSequence subject = contentCreator.getMessageSubject(newMessage);
        CharSequence summary = contentCreator.buildMessageSummary(sender, subject);

        boolean privacyModeEnabled = isPrivacyModeEnabled();

        if (privacyModeEnabled || summary.length() == 0) {
            summary = context.getString(R.string.notification_new_title);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(getNewMailNotificationIcon())
                .setColor(account.getChipColor())
                .setWhen(System.currentTimeMillis())
                .setNumber(unreadMessagesCount);

        if (!updateSilently) {
            builder.setTicker(summary);
        }

        String accountName = controller.getAccountName(account);
        ArrayList<MessageReference> allNotificationMessageReferences =
                notificationsHolder.getMessageReferencesForAllNotifications();

        if (NotificationController.platformSupportsExtendedNotifications() && !privacyModeEnabled) {
            boolean singleMessageNotification = (newMessagesCount == 1);
            if (singleMessageNotification) {
                createSingleMessageNotification(account, newMessage, sender, subject, builder,
                        accountName, allNotificationMessageReferences);
            } else {
                createInboxStyleNotification(account, notificationsHolder, builder, newMessagesCount, accountName);
            }

            addMarkAsReadAction(builder, account, allNotificationMessageReferences);

            if (isDeleteActionEnabled(singleMessageNotification)) {
                addDeleteAction(builder, account, allNotificationMessageReferences);
            }
        } else {
            String accountNotice = context.getString(R.string.notification_new_one_account_fmt,
                    unreadMessagesCount, accountName);

            builder.setContentTitle(accountNotice);
            builder.setContentText(summary);
        }

        for (Message message : notificationsHolder.getMessagesForSummaryNotification()) {
            if (message.isSet(Flag.FLAGGED)) {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                break;
            }
        }

        PendingIntent notificationPendingIntent = actionCreator.createSummaryNotificationActionPendingIntent(
                account, newMessage, newMessagesCount, unreadMessagesCount, allNotificationMessageReferences);
        builder.setContentIntent(notificationPendingIntent);

        PendingIntent deletePendingIntent = NotificationActionService.createDismissPendingIntent(
                context, account, account.getAccountNumber());
        builder.setDeleteIntent(deletePendingIntent);

        boolean ringAndVibrate = false;
        if (!updateSilently && !account.isRingNotified()) {
            account.setRingNotified(true);
            ringAndVibrate = true;
        }

        configureLockScreenNotification(builder, account, newMessagesCount, unreadMessagesCount, accountName,
                sender, notificationsHolder.getMessagesForSummaryNotification());

        NotificationSetting notificationSetting = account.getNotificationSetting();
        controller.configureNotification(
                builder,
                (notificationSetting.shouldRing()) ? notificationSetting.getRingtone() : null,
                (notificationSetting.shouldVibrate()) ? notificationSetting.getVibration() : null,
                (notificationSetting.isLed()) ? notificationSetting.getLedColor() : null,
                NOTIFICATION_LED_BLINK_SLOW,
                ringAndVibrate);

        int notificationId = NotificationIds.getNewMailNotificationId(account);
        getNotificationManager().notify(notificationId, builder.build());
    }

    private void createInboxStyleNotification(Account account, NotificationsHolder notificationsHolder, Builder builder,
            int newMessagesCount, String accountName) {

        String title = context.getResources().getQuantityString(R.plurals.notification_new_messages_title,
                newMessagesCount, newMessagesCount);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(builder);
        style.setBigContentTitle(title);

        if (notificationsHolder.hasAdditionalMessages()) {
            String summaryText = context.getString(R.string.notification_additional_messages,
                    notificationsHolder.getAdditionalMessagesCount(), accountName);

            style.setSummaryText(summaryText);
        }

        int notificationOffset = 1;
        for (LocalMessage message : notificationsHolder.getMessagesForSummaryNotification()) {
            MessageReference messageReference = message.makeMessageReference();

            CharSequence sender = contentCreator.getMessageSender(account, message);
            CharSequence subject = contentCreator.getMessageSubject(message);

            style.addLine(contentCreator.buildMessageSummary(sender, subject));

            Builder stackedNotificationBuilder = new Builder(context)
                    .setSmallIcon(R.drawable.ic_notify_new_mail)
                    .setWhen(System.currentTimeMillis())
                    .setGroup(NOTIFICATION_GROUP_KEY)
                    .setAutoCancel(true);

            int notificationId;
            Integer existingNotificationId = notificationsHolder.getStackedChildNotification(messageReference);
            if (existingNotificationId != null) {
                notificationId = existingNotificationId;
            } else {
                notificationId = NotificationIds.getNewMailNotificationId(account, notificationOffset);
            }

            setNotificationContent(message, sender, subject, stackedNotificationBuilder, accountName);

            addWearActionsForSingleMessage(stackedNotificationBuilder, account, message, notificationId);

            if (message.isSet(Flag.FLAGGED)) {
                stackedNotificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
            }

            notificationsHolder.addStackedChildNotification(messageReference, notificationId);
            getNotificationManager().notify(notificationId, stackedNotificationBuilder.build());

            notificationOffset++;
        }

        builder.setGroup(NOTIFICATION_GROUP_KEY)
                .setGroupSummary(true)
                .setLocalOnly(true)
                .setContentTitle(title)
                .setSubText(accountName)
                .setStyle(style);
    }

    private void createSingleMessageNotification(Account account, LocalMessage message,
            CharSequence sender, CharSequence subject, Builder builder, String accountName,
            ArrayList<MessageReference> allNotificationMessageReferences) {

        setNotificationContent(message, sender, subject, builder, accountName);

        addReplyAction(builder, account, message);

        addWearActions(builder, account, allNotificationMessageReferences,
                account.getAccountNumber());
    }

    private NotificationCompat.Builder setNotificationContent(Message message, CharSequence sender,
            CharSequence subject, Builder builder, String accountName) {

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);

        CharSequence preview = contentCreator.getMessagePreview(message);
        if (preview != null) {
            style.bigText(preview);
        }

        builder.setStyle(style)
                .setContentTitle(sender)
                .setContentText(subject)
                .setSubText(accountName);

        return builder;
    }

    private void addMarkAsReadAction(Builder builder, Account account,
            ArrayList<MessageReference> allNotificationMessageReferences) {

        int icon = getMarkAsReadActionIcon();
        String title = context.getString(R.string.notification_action_mark_as_read);
        PendingIntent readAllMessagesPendingIntent = NotificationActionService.createMarkAsReadPendingIntent(
                context, account, allNotificationMessageReferences, account.getAccountNumber());

        builder.addAction(icon, title, readAllMessagesPendingIntent);
    }

    private void addDeleteAction(Builder builder, Account account,
            ArrayList<MessageReference> allNotificationMessageReferences) {

        int icon = getDeleteActionIcon();
        String title = context.getString(R.string.notification_action_delete);
        PendingIntent confirmDeletionPendingIntent = NotificationDeleteConfirmation.getIntent(
                context, account, allNotificationMessageReferences, account.getAccountNumber());

        builder.addAction(icon, title, confirmDeletionPendingIntent);
    }

    private void addReplyAction(Builder builder, Account account, LocalMessage message) {
        int icon = getReplyActionIcon();
        String title = context.getString(R.string.notification_action_reply);
        PendingIntent replyToMessagePendingIntent = NotificationActionService.createReplyPendingIntent(
                context, account, message.makeMessageReference(), account.getAccountNumber());

        builder.addAction(icon, title, replyToMessagePendingIntent);
    }

    private void addWearActionsForSingleMessage(Builder builder, Account account, LocalMessage message,
            int notificationId) {
        ArrayList<MessageReference> allNotificationMessageReferences = new ArrayList<MessageReference>(1);
        allNotificationMessageReferences.add(message.makeMessageReference());

        addWearActions(builder, account, allNotificationMessageReferences, notificationId);
    }

    private void addWearActions(Builder builder, Account account,
            ArrayList<MessageReference> allNotificationMessageReferences, int notificationId) {

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

        if (isDeleteActionAvailableForWear()) {
            addWearDeleteAction(builder, account, allNotificationMessageReferences, notificationId, wearableExtender);
        }

        if (isArchiveActionAvailableForWear(account)) {
            addWearArchiveAction(builder, account, allNotificationMessageReferences, notificationId, wearableExtender);
        }

        if (isSpamActionAvailableForWear(account)) {
            addWearSpamAction(builder, account, allNotificationMessageReferences, notificationId, wearableExtender);
        }
    }

    private void addWearDeleteAction(Builder builder, Account account,
            ArrayList<MessageReference> allNotificationMessageReferences, int notificationId,
            WearableExtender wearableExtender) {

        int icon = R.drawable.ic_action_delete_dark;
        String title = context.getString(R.string.notification_action_delete);
        PendingIntent deleteMessagePendingIntent = NotificationDeleteConfirmation.getIntent(
                context, account, allNotificationMessageReferences, notificationId);

        NotificationCompat.Action wearDeleteAction =
                new NotificationCompat.Action.Builder(icon, title, deleteMessagePendingIntent).build();
        builder.extend(wearableExtender.addAction(wearDeleteAction));
    }

    private void addWearArchiveAction(Builder builder, Account account,
            ArrayList<MessageReference> allNotificationMessageReferences, int notificationId,
            WearableExtender wearableExtender) {

        int icon = R.drawable.ic_action_archive_dark;
        String title = context.getString(R.string.notification_action_archive);
        PendingIntent archiveMessagePendingIntent = NotificationActionService.createArchivePendingIntent(
                context, account, allNotificationMessageReferences, notificationId);

        NotificationCompat.Action wearActionArchive =
                new NotificationCompat.Action.Builder(icon, title, archiveMessagePendingIntent).build();
        builder.extend(wearableExtender.addAction(wearActionArchive));
    }

    private void addWearSpamAction(Builder builder, Account account,
            ArrayList<MessageReference> allNotificationMessageReferences, int notificationId,
            WearableExtender wearableExtender) {

        int icon = R.drawable.ic_action_delete_dark;
        String title = context.getString(R.string.notification_action_spam);
        PendingIntent markAsSpamPendingIntent = NotificationActionService.createMarkAsSpamPendingIntent(
                context, account, allNotificationMessageReferences, notificationId);

        NotificationCompat.Action wearActionSpam =
                new NotificationCompat.Action.Builder(icon, title, markAsSpamPendingIntent).build();
        builder.extend(wearableExtender.addAction(wearActionSpam));
    }

    private boolean isDeleteActionAvailableForWear() {
        return isDeleteActionEnabled(true) && !K9.confirmDeleteFromNotification();
    }

    private boolean isArchiveActionAvailableForWear(Account account) {
        String archiveFolderName = account.getArchiveFolderName();
        return archiveFolderName != null && isMovePossible(account, archiveFolderName);
    }

    private boolean isSpamActionAvailableForWear(Account account) {
        String spamFolderName = account.getSpamFolderName();
        return spamFolderName != null && !K9.confirmSpam() && isMovePossible(account, spamFolderName);
    }

    private boolean isMovePossible(Account account, String destinationFolderName) {
        if (K9.FOLDER_NONE.equalsIgnoreCase(destinationFolderName)) {
            return false;
        }

        MessagingController controller = MessagingController.getInstance(context);
        return controller.isMoveCapable(account);
    }

    private void configureLockScreenNotification(Builder builder, Account account, int newMessages, int unreadCount,
            CharSequence accountDescription, CharSequence formattedSender, List<LocalMessage> messages) {

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
                Builder publicNotification = createPublicNotification(account, newMessages, unreadCount);
                if (newMessages == 1) {
                    publicNotification.setContentText(formattedSender);
                } else {
                    String senderList = contentCreator.createCommaSeparatedListOfSenders(account, messages);
                    publicNotification.setContentText(senderList);
                }

                builder.setPublicVersion(publicNotification.build());
                break;
            }
            case MESSAGE_COUNT: {
                Builder publicNotification = createPublicNotification(account, newMessages, unreadCount);
                publicNotification.setContentText(accountDescription);

                builder.setPublicVersion(publicNotification.build());
                break;
            }
        }
    }

    private Builder createPublicNotification(Account account, int newMessages, int unreadCount) {
        String title = context.getResources().getQuantityString(R.plurals.notification_new_messages_title,
                newMessages, newMessages);

        return new Builder(context)
                .setSmallIcon(R.drawable.ic_notify_new_mail_vector)
                .setColor(account.getChipColor())
                .setNumber(unreadCount)
                .setContentTitle(title);
    }

    private boolean isPrivacyModeEnabled() {
        KeyguardManager keyguardService = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        boolean privacyModeAlwaysEnabled = K9.getNotificationHideSubject() == NotificationHideSubject.ALWAYS;
        boolean privacyModeEnabledWhenLocked = K9.getNotificationHideSubject() == NotificationHideSubject.WHEN_LOCKED;
        boolean screenLocked = keyguardService.inKeyguardRestrictedInputMode();

        return privacyModeAlwaysEnabled || (privacyModeEnabledWhenLocked && screenLocked);
    }

    private boolean isDeleteActionEnabled(boolean singleMessageNotification) {
        NotificationQuickDelete deleteOption = K9.getNotificationQuickDeleteBehaviour();

        return deleteOption == NotificationQuickDelete.ALWAYS ||
                (deleteOption == NotificationQuickDelete.FOR_SINGLE_MSG && singleMessageNotification);
    }

    private int getNewMailNotificationIcon() {
        return controller.platformSupportsVectorDrawables() ?
                R.drawable.ic_notify_new_mail_vector : R.drawable.ic_notify_new_mail;
    }

    private int getMarkAsReadActionIcon() {
        return controller.platformSupportsVectorDrawables() ?
                R.drawable.ic_action_mark_as_read_dark_vector : R.drawable.ic_action_mark_as_read_dark;
    }

    private int getDeleteActionIcon() {
        return NotificationController.platformSupportsLockScreenNotifications() ?
                R.drawable.ic_action_delete_dark_vector : R.drawable.ic_action_delete_dark;
    }

    private int getReplyActionIcon() {
        return controller.platformSupportsVectorDrawables() ?
                R.drawable.ic_action_single_message_options_dark_vector :
                R.drawable.ic_action_single_message_options_dark;
    }

    private NotificationManager getNotificationManager() {
        return controller.getNotificationManager();
    }
}
