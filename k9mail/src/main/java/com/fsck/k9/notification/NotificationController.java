package com.fsck.k9.notification;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.TaskStackBuilder;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.NotificationDeleteConfirmation;
import com.fsck.k9.activity.setup.AccountSetupIncoming;
import com.fsck.k9.activity.setup.AccountSetupOutgoing;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;


public class NotificationController {
    private static final boolean NOTIFICATION_LED_WHILE_SYNCING = false;
    private static final int MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5;

    private static final String NOTIFICATION_GROUP_KEY = "newMailNotifications";
    private static final int NOTIFICATION_LED_ON_TIME = 500;
    private static final int NOTIFICATION_LED_OFF_TIME = 2000;
    private static final int NOTIFICATION_LED_FAST_ON_TIME = 100;
    private static final int NOTIFICATION_LED_FAST_OFF_TIME = 100;
    private static final int NOTIFICATION_LED_BLINK_SLOW = 0;
    private static final int NOTIFICATION_LED_BLINK_FAST = 1;
    private static final int NOTIFICATION_LED_FAILURE_COLOR = 0xffff0000;


    private final Context context;
    private final NotificationManager notificationManager;
    private TextAppearanceSpan emphasizedSpan;

    private final ConcurrentMap<Integer, NotificationsHolder> notificationsHolders =
            new ConcurrentHashMap<Integer, NotificationsHolder>();


    public static NotificationController newInstance(Context context) {
        Context appContext = context.getApplicationContext();
        NotificationManager notificationManager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        return new NotificationController(appContext, notificationManager);
    }

    public static boolean platformSupportsExtendedNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean platformSupportsLockScreenNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }


    NotificationController(Context context, NotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
    }

    public void showCertificateErrorNotification(Account account, boolean incoming) {
        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, incoming);

        Intent editServerSettingsIntent = incoming ?
                AccountSetupIncoming.intentActionEditIncomingSettings(context, account) :
                AccountSetupOutgoing.intentActionEditOutgoingSettings(context, account);

        PendingIntent editServerSettingsPendingIntent = PendingIntent.getActivity(context,
                account.getAccountNumber(), editServerSettingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = context.getString(R.string.notification_certificate_error_title, account.getDescription());
        String text = context.getString(R.string.notification_certificate_error_text);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(getCertificateErrorNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(editServerSettingsPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        configureNotification(builder, null, null,
                NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        notificationManager.notify(notificationId, builder.build());
    }

    public void clearCertificateErrorNotifications(Account account, boolean incoming) {
        int notificationId = NotificationIds.getCertificateErrorNotificationId(account, incoming);
        notificationManager.cancel(notificationId);
    }

    public void showSendingNotification(Account account) {
        String accountName = getAccountName(account);
        String title = context.getString(R.string.notification_bg_send_title);
        String tickerText = context.getString(R.string.notification_bg_send_ticker, accountName);

        String folderName = account.getInboxFolderName();
        TaskStackBuilder stack = buildMessageListBackStack(account, folderName);
        PendingIntent showMessageListPendingIntent = stack.getPendingIntent(0, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notify_check_mail)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setTicker(tickerText)
                .setContentTitle(title)
                .setContentText(accountName)
                .setContentIntent(showMessageListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (NOTIFICATION_LED_WHILE_SYNCING) {
            configureNotification(builder, null, null,
                    account.getNotificationSetting().getLedColor(),
                    NOTIFICATION_LED_BLINK_FAST, true);
        }

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        notificationManager.notify(notificationId, builder.build());
    }

    public void clearSendingNotification(Account account) {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        notificationManager.cancel(notificationId);
    }

    public void showSendFailedNotification(Account account, Exception exception) {
        String title = context.getString(R.string.send_failure_subject);
        String text = getRootCauseMessage(exception);

        TaskStackBuilder stack = buildFolderListBackStack(account);
        PendingIntent folderListPendingIntent = stack.getPendingIntent(0, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(getSendFailedNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(folderListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        configureNotification(builder, null, null, NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        notificationManager.notify(notificationId, builder.build());
    }

    public void clearSendFailedNotification(Account account) {
        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        notificationManager.cancel(notificationId);
    }

    public void showFetchingMailNotification(Account account, Folder folder) {
        String accountName = account.getDescription();
        String folderName = folder.getName();

        String tickerText = context.getString(R.string.notification_bg_sync_ticker, accountName, folderName);
        String title = context.getString(R.string.notification_bg_sync_title);
        //TODO: Use format string from resources
        String text = accountName + context.getString(R.string.notification_bg_title_separator) + folderName;

        TaskStackBuilder stack = buildMessageListBackStack(account,
                account.getInboxFolderName());
        PendingIntent showMessageListPendingIntent = stack.getPendingIntent(0, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notify_check_mail)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setTicker(tickerText)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(showMessageListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (NOTIFICATION_LED_WHILE_SYNCING) {
            configureNotification(builder, null, null,
                    account.getNotificationSetting().getLedColor(),
                    NOTIFICATION_LED_BLINK_FAST, true);
        }

        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        notificationManager.notify(notificationId, builder.build());
    }

    public void clearFetchingMailNotification(Account account) {
        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
        notificationManager.cancel(notificationId);
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
                    notificationManager.cancel(childNotification);
                }

                notifyAccountWithDataLocked(account, null, notificationsHolder);
            }
        }
    }

    public void clearNewMailNotifications(Account account) {
        int newMailNotificationId = NotificationIds.getNewMailNotificationId(account);
        notificationManager.cancel(newMailNotificationId);

        notificationsHolders.remove(account.getAccountNumber());

        //TODO: clear stacked notifications
    }

    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
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
        CharSequence sender = getMessageSender(account, newMessage);
        CharSequence subject = getMessageSubject(newMessage);
        CharSequence summary = buildMessageSummary(sender, subject);

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

        String accountName = getAccountName(account);
        ArrayList<MessageReference> allNotificationMessageReferences =
                notificationsHolder.getMessageReferencesForAllNotifications();

        if (platformSupportsExtendedNotifications() && !privacyModeEnabled) {
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

        TaskStackBuilder stack = buildNotificationNavigationStack(account, newMessage, newMessagesCount,
                unreadMessagesCount, allNotificationMessageReferences);
        PendingIntent notificationPendingIntent = stack.getPendingIntent(account.getAccountNumber(),
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
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
        configureNotification(
                builder,
                (notificationSetting.shouldRing()) ? notificationSetting.getRingtone() : null,
                (notificationSetting.shouldVibrate()) ? notificationSetting.getVibration() : null,
                (notificationSetting.isLed()) ? notificationSetting.getLedColor() : null,
                NOTIFICATION_LED_BLINK_SLOW,
                ringAndVibrate);

        int notificationId = NotificationIds.getNewMailNotificationId(account);
        notificationManager.notify(notificationId, builder.build());
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

            CharSequence sender = getMessageSender(account, message);
            CharSequence subject = getMessageSubject(message);

            style.addLine(buildMessageSummary(sender, subject));

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
            notificationManager.notify(notificationId, stackedNotificationBuilder.build());

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

        CharSequence preview = getMessagePreview(message);
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

    private void configureNotification(NotificationCompat.Builder builder, String ringtone,
            long[] vibrationPattern, Integer ledColor, int ledSpeed, boolean ringAndVibrate) {

        if (K9.isQuietTime()) {
            return;
        }

        if (ringAndVibrate) {
            if (ringtone != null && !TextUtils.isEmpty(ringtone)) {
                builder.setSound(Uri.parse(ringtone));
            }

            if (vibrationPattern != null) {
                builder.setVibrate(vibrationPattern);
            }
        }

        if (ledColor != null) {
            int ledOnMS;
            int ledOffMS;
            if (ledSpeed == NOTIFICATION_LED_BLINK_SLOW) {
                ledOnMS = NOTIFICATION_LED_ON_TIME;
                ledOffMS = NOTIFICATION_LED_OFF_TIME;
            } else {
                ledOnMS = NOTIFICATION_LED_FAST_ON_TIME;
                ledOffMS = NOTIFICATION_LED_FAST_OFF_TIME;
            }

            builder.setLights(ledColor, ledOnMS, ledOffMS);
        }
    }

    private void configureLockScreenNotification(Builder builder, Account account, int newMessages, int unreadCount,
            CharSequence accountDescription, CharSequence formattedSender, List<LocalMessage> messages) {

        if (!platformSupportsLockScreenNotifications()) {
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
                    String senderList = createCommaSeparatedListOfSenders(account, messages);
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

    private boolean platformSupportsVectorDrawables() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private int getNewMailNotificationIcon() {
        return platformSupportsVectorDrawables() ? R.drawable.ic_notify_new_mail_vector : R.drawable.ic_notify_new_mail;
    }

    private int getMarkAsReadActionIcon() {
        return platformSupportsVectorDrawables() ?
                R.drawable.ic_action_mark_as_read_dark_vector : R.drawable.ic_action_mark_as_read_dark;
    }

    private int getDeleteActionIcon() {
        return platformSupportsLockScreenNotifications() ?
                R.drawable.ic_action_delete_dark_vector : R.drawable.ic_action_delete_dark;
    }

    private int getReplyActionIcon() {
        return platformSupportsVectorDrawables() ?
                R.drawable.ic_action_single_message_options_dark_vector :
                R.drawable.ic_action_single_message_options_dark;
    }

    private int getCertificateErrorNotificationIcon() {
        //TODO: Use a different icon for certificate error notifications
        return getNewMailNotificationIcon();
    }

    private int getSendFailedNotificationIcon() {
        //TODO: Use a different icon for send failure notifications
        return getNewMailNotificationIcon();
    }

    private TaskStackBuilder buildNotificationNavigationStack(Account account, LocalMessage message, int newMessages,
            int unreadCount, ArrayList<MessageReference> allRefs) {

        TaskStackBuilder stack;
        boolean treatAsSingleMessageNotification;

        if (platformSupportsExtendedNotifications()) {
            // in the new-style notifications, we focus on the new messages, not the unread ones
            treatAsSingleMessageNotification = newMessages == 1;
        } else {
            // in the old-style notifications, we focus on unread messages, as we don't have a
            // good way to express the new message count
            treatAsSingleMessageNotification = unreadCount == 1;
        }

        if (treatAsSingleMessageNotification) {
            stack = buildMessageViewBackStack(message.makeMessageReference());
        } else if (account.goToUnreadMessageSearch()) {
            stack = buildUnreadBackStack(account);
        } else {
            String initialFolder = message.getFolder().getName();
            /* only go to folder if all messages are in the same folder, else go to folder list */
            for (MessageReference ref : allRefs) {
                if (!TextUtils.equals(initialFolder, ref.getFolderName())) {
                    initialFolder = null;
                    break;
                }
            }

            stack = buildMessageListBackStack(account, initialFolder);
        }
        return stack;
    }

    private TaskStackBuilder buildAccountsBackStack() {
        TaskStackBuilder stack = TaskStackBuilder.create(context);
        if (!skipAccountsInBackStack()) {
            stack.addNextIntent(new Intent(context, Accounts.class).putExtra(Accounts.EXTRA_STARTUP, false));
        }
        return stack;
    }

    private TaskStackBuilder buildFolderListBackStack(Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();
        stack.addNextIntent(FolderList.actionHandleAccountIntent(context, account, false));
        return stack;
    }

    private TaskStackBuilder buildUnreadBackStack(final Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();
        LocalSearch search = Accounts.createUnreadSearch(context, account);
        stack.addNextIntent(MessageList.intentDisplaySearch(context, search, true, false, false));
        return stack;
    }

    private TaskStackBuilder buildMessageListBackStack(Account account, String folder) {
        TaskStackBuilder stack = skipFolderListInBackStack(account, folder) ?
                buildAccountsBackStack() : buildFolderListBackStack(account);

        if (folder != null) {
            LocalSearch search = new LocalSearch(folder);
            search.addAllowedFolder(folder);
            search.addAccountUuid(account.getUuid());
            stack.addNextIntent(MessageList.intentDisplaySearch(context, search, false, true, true));
        }
        return stack;
    }

    private TaskStackBuilder buildMessageViewBackStack(MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.getAccountUuid());
        TaskStackBuilder stack = buildMessageListBackStack(account, message.getFolderName());
        stack.addNextIntent(MessageList.actionDisplayMessageIntent(context, message));
        return stack;
    }

    private boolean skipFolderListInBackStack(Account account, String folder) {
        return folder != null && folder.equals(account.getAutoExpandFolderName());
    }

    private boolean skipAccountsInBackStack() {
        return Preferences.getPreferences(context).getAccounts().size() == 1;
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

    private String getAccountName(Account account) {
        String accountDescription = account.getDescription();
        return TextUtils.isEmpty(accountDescription) ? account.getEmail() : accountDescription;
    }

    private CharSequence getMessageSubject(Message message) {
        String subject = message.getSubject();
        if (!TextUtils.isEmpty(subject)) {
            return subject;
        }

        return context.getString(R.string.general_no_subject);
    }

    private TextAppearanceSpan getEmphasizedSpan() {
        if (emphasizedSpan == null) {
            emphasizedSpan = new TextAppearanceSpan(context,
                    R.style.TextAppearance_StatusBar_EventContent_Emphasized);
        }
        return emphasizedSpan;
    }

    private CharSequence getMessageSender(Account account, Message message) {
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

    private String createCommaSeparatedListOfSenders(Account account, List<LocalMessage> messages) {
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

    private CharSequence getMessagePreview(Message message) {
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

    private CharSequence buildMessageSummary(CharSequence sender, CharSequence subject) {
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

    private String getRootCauseMessage(Throwable t) {
        Throwable rootCause = t;
        Throwable nextCause;
        do {
            nextCause = rootCause.getCause();
            if (nextCause != null) {
                rootCause = nextCause;
            }
        } while (nextCause != null);

        if (rootCause instanceof MessagingException) {
            return rootCause.getMessage();
        }

        // Remove the namespace on the exception so we have a fighting chance of seeing more of the error in the
        // notification.
        String simpleName = rootCause.getClass().getSimpleName();
        return (rootCause.getLocalizedMessage() != null) ?
                simpleName + ": " + rootCause.getLocalizedMessage() : simpleName;
    }
}
