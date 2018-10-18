package com.fsck.k9.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class NotificationChannelUtils {
    private static final String PREFIX_MESSAGES = "messages_";
    private static final String PREFIX_OTHER = "other_";

    public enum Type {
        MESSAGES, OTHER
    }

    public static void updateChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            List<Account> accounts = Preferences.getPreferences(context).getAccounts();
            removeChannelsForNonExistingOrChangedAccounts(notificationManager, accounts);
            addChannelsForAccounts(context, notificationManager, accounts);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void addChannelsForAccounts(Context context, NotificationManager notificationManager, List<Account> accounts) {
        for (Account account : accounts) {
            final String groupId = account.getUuid();
            NotificationChannelGroup group = new NotificationChannelGroup(groupId, account.getDisplayName());

            NotificationChannel channelNewMail = getChannelMessages(context, groupId);
            NotificationChannel channelOther = getChannelOther(context, groupId);
            channelNewMail.setGroup(groupId);
            channelOther.setGroup(groupId);

            notificationManager.createNotificationChannelGroup(group);
            notificationManager.createNotificationChannel(channelNewMail);
            notificationManager.createNotificationChannel(channelOther);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void removeChannelsForNonExistingOrChangedAccounts(NotificationManager notificationManager, List<Account> accounts) {
        HashMap<String, Account> existingAccounts = new HashMap<>();
        for (Account account : accounts) {
            existingAccounts.put(account.getUuid(), account);
        }

        List<NotificationChannelGroup> groups = notificationManager.getNotificationChannelGroups();
        for (NotificationChannelGroup group : groups) {
            final String groupId = group.getId();

            boolean shouldDelete = false;
            if (!existingAccounts.containsKey(groupId)) {
                shouldDelete = true;
            } else if (!existingAccounts.get(groupId).getName().equals(group.getName().toString())) {
                // There is no way to change group names. Deleting group, so it is re-generated.
                shouldDelete = true;
            }

            if (shouldDelete) {
                notificationManager.deleteNotificationChannelGroup(groupId);
                notificationManager.deleteNotificationChannel(PREFIX_MESSAGES + groupId);
                notificationManager.deleteNotificationChannel(PREFIX_OTHER + groupId);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel getChannelMessages(Context context, String accountId) {
        NotificationChannel mChannel = new NotificationChannel(PREFIX_MESSAGES + accountId,
                "Messages", NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription("When receiving new messages");
        return mChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel getChannelOther(Context context, String accountId) {
        NotificationChannel mChannel = new NotificationChannel(PREFIX_OTHER + accountId,
                "Others", NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription("Other notifications like errors");
        return mChannel;
    }

    public static String getChannelIdFor(Account account, Type type) {
        if (type == Type.MESSAGES) {
            return PREFIX_MESSAGES + account.getUuid();
        } else {
            return PREFIX_OTHER + account.getUuid();
        }
    }
}
