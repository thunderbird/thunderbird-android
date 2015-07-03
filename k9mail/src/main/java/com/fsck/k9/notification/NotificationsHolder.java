package com.fsck.k9.notification;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;


/**
 * A holder class for pending new mail notifications.
 */
class NotificationsHolder {
    // Note: As of Jellybean, phone notifications show a maximum of 5 lines, while tablet notifications show 7 lines.
    private static final int MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION = 5;


    private int unreadMessagesCountBeforeNotification;
    private LinkedList<LocalMessage> messagesForSummaryNotification;
    private LinkedList<MessageReference> additionalMessages;
    private Map<MessageReference, Integer> stackedNotifications = new HashMap<MessageReference, Integer>();


    public NotificationsHolder(int unreadMessagesCount) {
        unreadMessagesCountBeforeNotification = unreadMessagesCount;
        additionalMessages = new LinkedList<MessageReference>();
        messagesForSummaryNotification = new LinkedList<LocalMessage>();
    }

    public int getUnreadMessagesCountBeforeNotification() {
        return unreadMessagesCountBeforeNotification;
    }

    public List<LocalMessage> getMessagesForSummaryNotification() {
        return Collections.unmodifiableList(messagesForSummaryNotification);
    }

    public boolean hasAdditionalMessages() {
        return !additionalMessages.isEmpty();
    }

    public int getAdditionalMessagesCount() {
        return additionalMessages.size();
    }

    public void addMessage(LocalMessage message) {
        while (messagesForSummaryNotification.size() >= MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION) {
            LocalMessage dropped = messagesForSummaryNotification.removeLast();
            additionalMessages.addFirst(dropped.makeMessageReference());
        }

        messagesForSummaryNotification.addFirst(message);
    }

    public void addStackedChildNotification(MessageReference messageReference, final int notificationId) {
        stackedNotifications.put(messageReference, notificationId);
    }

    public Integer getStackedChildNotification(MessageReference messageReference) {
        return stackedNotifications.get(messageReference);
    }

    public boolean removeMatchingMessage(Context context, MessageReference messageReference) {
        if (additionalMessages.remove(messageReference)) {
            return true;
        }

        for (LocalMessage message : messagesForSummaryNotification) {
            if (message.makeMessageReference().equals(messageReference)) {
                if (messagesForSummaryNotification.remove(message) && !additionalMessages.isEmpty()) {
                    LocalMessage restoredMessage = additionalMessages.getFirst().restoreToLocalMessage(context);
                    if (restoredMessage != null) {
                        messagesForSummaryNotification.addLast(restoredMessage);
                        additionalMessages.removeFirst();
                    }
                }
                return true;
            }
        }

        return false;
    }

    public ArrayList<MessageReference> getMessageReferencesForAllNotifications() {
        int size = messagesForSummaryNotification.size() + additionalMessages.size();
        ArrayList<MessageReference> messageReferences = new ArrayList<MessageReference>(size);

        for (LocalMessage message : messagesForSummaryNotification) {
            messageReferences.add(message.makeMessageReference());
        }

        messageReferences.addAll(additionalMessages);

        return messageReferences;
    }

    public int getNewMessagesCount() {
        return messagesForSummaryNotification.size() + additionalMessages.size();
    }

    public LocalMessage getNewestMessage(Context context) {
        if (!messagesForSummaryNotification.isEmpty()) {
            return messagesForSummaryNotification.getFirst();
        }

        if (!additionalMessages.isEmpty()) {
            return additionalMessages.getFirst().restoreToLocalMessage(context);
        }

        return null;
    }
}
