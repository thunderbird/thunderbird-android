package com.fsck.k9.notification;


import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.util.SparseBooleanArray;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageReference;


/**
 * A holder class for pending new mail notifications.
 */
class NotificationData {
    // Note: As of Jellybean, phone notifications show a maximum of 5 lines, while tablet notifications show 7 lines.
    static final int MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION = 5;
    // Note: This class assumes MAX_NUMBER_OF_STACKED_NOTIFICATIONS >= MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION
    static final int MAX_NUMBER_OF_STACKED_NOTIFICATIONS = 8;


    private final Account account;
    private final LinkedList<NotificationHolder> activeNotifications = new LinkedList<NotificationHolder>();
    private final Deque<NotificationContent> additionalNotifications = new LinkedList<NotificationContent>();
    private final SparseBooleanArray notificationIdsInUse = new SparseBooleanArray();
    private int unreadMessageCount;


    public NotificationData(Account account) {
        this.account = account;
    }

    public AddNotificationResult addNotificationContent(NotificationContent content) {
        int notificationId;
        boolean cancelNotificationIdBeforeReuse;
        if (isMaxNumberOfActiveNotificationsReached()) {
            NotificationHolder notificationHolder = activeNotifications.removeLast();
            addToAdditionalNotifications(notificationHolder);
            notificationId = notificationHolder.notificationId;
            cancelNotificationIdBeforeReuse = true;
        } else {
            notificationId = getNewNotificationId();
            cancelNotificationIdBeforeReuse = false;
        }

        NotificationHolder notificationHolder = createNotificationHolder(notificationId, content);
        activeNotifications.addFirst(notificationHolder);

        if (cancelNotificationIdBeforeReuse) {
            return AddNotificationResult.replaceNotification(notificationHolder);
        } else {
            return AddNotificationResult.newNotification(notificationHolder);
        }
    }

    private boolean isMaxNumberOfActiveNotificationsReached() {
        return activeNotifications.size() == MAX_NUMBER_OF_STACKED_NOTIFICATIONS;
    }

    private void addToAdditionalNotifications(NotificationHolder notificationHolder) {
        additionalNotifications.addFirst(notificationHolder.content);
    }

    private int getNewNotificationId() {
        for (int i = 0; i < MAX_NUMBER_OF_STACKED_NOTIFICATIONS; i++) {
            int notificationId = NotificationIds.getNewMailStackedNotificationId(account, i);
            if (!isNotificationInUse(notificationId)) {
                markNotificationIdAsInUse(notificationId);
                return notificationId;
            }
        }

        throw new AssertionError("getNewNotificationId() called with no free notification ID");
    }

    private boolean isNotificationInUse(int notificationId) {
        return notificationIdsInUse.get(notificationId);
    }

    private void markNotificationIdAsInUse(int notificationId) {
        notificationIdsInUse.put(notificationId, true);
    }

    private void markNotificationIdAsFree(int notificationId) {
        notificationIdsInUse.delete(notificationId);
    }

    NotificationHolder createNotificationHolder(int notificationId, NotificationContent content) {
        return new NotificationHolder(notificationId, content);
    }

    public boolean containsStarredMessages() {
        for (NotificationHolder holder : activeNotifications) {
            if (holder.content.starred) {
                return true;
            }
        }

        for (NotificationContent content : additionalNotifications) {
            if (content.starred) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAdditionalMessages() {
        return activeNotifications.size() > MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION;
    }

    public int getAdditionalMessagesCount() {
        return additionalNotifications.size();
    }

    public int getNewMessagesCount() {
        return activeNotifications.size() + additionalNotifications.size();
    }

    public boolean isSingleMessageNotification() {
        return activeNotifications.size() == 1;
    }

    public NotificationHolder getHolderForLatestNotification() {
        return activeNotifications.getFirst();
    }

    public List<NotificationContent> getContentForSummaryNotification() {
        int size = calculateNumberOfMessagesForSummaryNotification();
        List<NotificationContent> result = new ArrayList<NotificationContent>(size);

        Iterator<NotificationHolder> iterator = activeNotifications.iterator();
        int notificationCount = 0;
        while (iterator.hasNext() && notificationCount < MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION) {
            NotificationHolder holder = iterator.next();
            result.add(holder.content);
            notificationCount++;
        }

        return result;
    }

    private int calculateNumberOfMessagesForSummaryNotification() {
        return Math.min(activeNotifications.size(), MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION);
    }

    public int[] getActiveNotificationIds() {
        int size = activeNotifications.size();
        int[] notificationIds = new int[size];

        for (int i = 0; i < size; i++) {
            NotificationHolder holder = activeNotifications.get(i);
            notificationIds[i] = holder.notificationId;
        }

        return notificationIds;
    }

    public RemoveNotificationResult removeNotificationForMessage(MessageReference messageReference) {
        NotificationHolder holder = getNotificationHolderForMessage(messageReference);
        if (holder == null) {
            return RemoveNotificationResult.unknownNotification();
        }

        activeNotifications.remove(holder);

        int notificationId = holder.notificationId;
        markNotificationIdAsFree(notificationId);

        if (!additionalNotifications.isEmpty()) {
            NotificationContent newContent = additionalNotifications.removeFirst();
            NotificationHolder replacement = createNotificationHolder(notificationId, newContent);
            activeNotifications.addLast(replacement);
            return RemoveNotificationResult.createNotification(replacement);
        }

        return RemoveNotificationResult.cancelNotification(notificationId);
    }

    private NotificationHolder getNotificationHolderForMessage(MessageReference messageReference) {
        for (NotificationHolder holder : activeNotifications) {
            if (messageReference.equals(holder.content.messageReference)) {
                return holder;
            }
        }

        return null;
    }

    public Account getAccount() {
        return account;
    }

    public int getUnreadMessageCount() {
        return unreadMessageCount + getNewMessagesCount();
    }

    public void setUnreadMessageCount(int unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
    }

    public ArrayList<MessageReference> getAllMessageReferences() {
        int newSize = activeNotifications.size() + additionalNotifications.size();
        ArrayList<MessageReference> messageReferences = new ArrayList<MessageReference>(newSize);

        for (NotificationHolder holder : activeNotifications) {
            messageReferences.add(holder.content.messageReference);
        }

        for (NotificationContent content : additionalNotifications) {
            messageReferences.add(content.messageReference);
        }

        return messageReferences;
    }
}
