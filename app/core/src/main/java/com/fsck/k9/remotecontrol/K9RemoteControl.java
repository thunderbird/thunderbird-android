package com.fsck.k9.remotecontrol;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/**
 * Utility definitions for Android applications to control the behavior of K-9 Mail.  All such applications must declare the following permission:
 * <uses-permission android:name="com.fsck.k9.permission.REMOTE_CONTROL"/>
 * in their AndroidManifest.xml  In addition, all applications sending remote control messages to K-9 Mail must
 *
 * An application that wishes to act on a particular Account in K-9 needs to fetch the list of configured Accounts by broadcasting an
 * {@link Intent} using K9_REQUEST_ACCOUNTS as the Action.  The broadcast must be made using the {@link ContextWrapper}
 * sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
 * Handler scheduler, int initialCode, String initialData, Bundle initialExtras).sendOrderedBroadcast}
 * method in order to receive the list of Account UUIDs and descriptions that K-9 will provide.
 *
 * @author Daniel I. Applebaum
 *
 */
public class K9RemoteControl {
    /**
     * Permission that every application sending a broadcast to K-9 for Remote Control purposes should send on every broadcast.
     * Prevent other applications from intercepting the broadcasts.
     */
    public static String K9_REMOTE_CONTROL_PERMISSION;
    /**
     * {@link Intent} Action to be sent to K-9 using {@link ContextWrapper.sendOrderedBroadcast} in order to fetch the list of configured Accounts.
     * The responseData will contain two String[] with keys K9_ACCOUNT_UUIDS and K9_ACCOUNT_DESCRIPTIONS
     */
    public static String K9_REQUEST_ACCOUNTS;
    public static String K9_ACCOUNT_UUIDS;
    public static String K9_ACCOUNT_DESCRIPTIONS;

    /**
     * The {@link {@link Intent}} Action to set in order to cause K-9 to check mail.  (Not yet implemented)
     */
    //public final static String K9_CHECK_MAIL = "com.fsck.k9.K9RemoteControl.checkMail";

    /**
     * The {@link {@link Intent}} Action to set when remotely changing K-9 Mail settings
     */
    public static String K9_SET;
    /**
     * The key of the {@link Intent} Extra to set to hold the UUID of a single Account's settings to change.  Used only if K9_ALL_ACCOUNTS
     * is absent or false.
     */
    public static String K9_ACCOUNT_UUID;
    /**
     * The key of the {@link Intent} Extra to set to control if the settings will apply to all Accounts, or to the one
     * specified with K9_ACCOUNT_UUID
     */
    public static String K9_ALL_ACCOUNTS;

    public final static String K9_ENABLED = "true";
    public final static String K9_DISABLED = "false";

    /*
     * Key for the {@link Intent} Extra for controlling whether notifications will be generated for new unread mail.
     * Acceptable values are K9_ENABLED and K9_DISABLED
     */
    public static String K9_NOTIFICATION_ENABLED;
    /*
     * Key for the {@link Intent} Extra for controlling whether K-9 will sound the ringtone for new unread mail.
     * Acceptable values are K9_ENABLED and K9_DISABLED
     */
    public static String K9_RING_ENABLED;
    /*
     * Key for the {@link Intent} Extra for controlling whether K-9 will activate the vibrator for new unread mail.
     * Acceptable values are K9_ENABLED and K9_DISABLED
     */
    public static String K9_VIBRATE_ENABLED;

    public final static String K9_FOLDERS_NONE = "NONE";
    public final static String K9_FOLDERS_ALL = "ALL";
    public final static String K9_FOLDERS_FIRST_CLASS = "FIRST_CLASS";
    public final static String K9_FOLDERS_FIRST_AND_SECOND_CLASS = "FIRST_AND_SECOND_CLASS";
    public final static String K9_FOLDERS_NOT_SECOND_CLASS = "NOT_SECOND_CLASS";
    /**
     * Key for the {@link Intent} Extra to set for controlling which folders to be synchronized with Push.
     * Acceptable values are K9_FOLDERS_ALL, K9_FOLDERS_FIRST_CLASS, K9_FOLDERS_FIRST_AND_SECOND_CLASS,
     * K9_FOLDERS_NOT_SECOND_CLASS, K9_FOLDERS_NONE
     */
    public static String K9_PUSH_CLASSES;
    /**
     * Key for the {@link Intent} Extra to set for controlling which folders to be synchronized with Poll.
     * Acceptable values are K9_FOLDERS_ALL, K9_FOLDERS_FIRST_CLASS, K9_FOLDERS_FIRST_AND_SECOND_CLASS,
     * K9_FOLDERS_NOT_SECOND_CLASS, K9_FOLDERS_NONE
     */
    public static String K9_POLL_CLASSES;

    public final static String[] K9_POLL_FREQUENCIES = { "-1", "1", "5", "10", "15", "30", "60", "120", "180", "360", "720", "1440"};
    /**
     * Key for the {@link Intent} Extra to set with the desired poll frequency.  The value is a String representing a number of minutes.
     * Acceptable values are available in K9_POLL_FREQUENCIES
     */
    public static String K9_POLL_FREQUENCY;

    /**
     * Key for the {@link Intent} Extra to set for controlling K-9's global "Background sync" setting.
     * Acceptable values are K9_BACKGROUND_OPERATIONS_ALWAYS, K9_BACKGROUND_OPERATIONS_NEVER
     * K9_BACKGROUND_OPERATIONS_WHEN_CHECKED_AUTO_SYNC
     */
    public static String K9_BACKGROUND_OPERATIONS;
    public final static String K9_BACKGROUND_OPERATIONS_ALWAYS = "ALWAYS";
    public final static String K9_BACKGROUND_OPERATIONS_NEVER = "NEVER";
    public final static String K9_BACKGROUND_OPERATIONS_WHEN_CHECKED_AUTO_SYNC = "WHEN_CHECKED_AUTO_SYNC";

    /**
     * Key for the {@link Intent} Extra to set for controlling which display theme K-9 will use.  Acceptable values are
     * K9_THEME_LIGHT, K9_THEME_DARK
     */
    public static String K9_THEME;
    public final static String K9_THEME_LIGHT = "LIGHT";
    public final static String K9_THEME_DARK = "DARK";

    protected static final String LOG_TAG = "K9RemoteControl";

    public static void init(String packageName) {
        K9_REMOTE_CONTROL_PERMISSION = packageName + ".permission.REMOTE_CONTROL";
        K9_REQUEST_ACCOUNTS = packageName + ".K9RemoteControl.requestAccounts";
        K9_ACCOUNT_UUIDS = packageName + ".K9RemoteControl.accountUuids";
        K9_ACCOUNT_DESCRIPTIONS = packageName + ".K9RemoteControl.accountDescriptions";
        K9_SET = packageName + ".K9RemoteControl.set";
        K9_ACCOUNT_UUID = packageName + ".K9RemoteControl.accountUuid";
        K9_ALL_ACCOUNTS = packageName + ".K9RemoteControl.allAccounts";
        K9_NOTIFICATION_ENABLED = packageName + ".K9RemoteControl.notificationEnabled";
        K9_RING_ENABLED = packageName + ".K9RemoteControl.ringEnabled";
        K9_VIBRATE_ENABLED = packageName + ".K9RemoteControl.vibrateEnabled";
        K9_PUSH_CLASSES = packageName + ".K9RemoteControl.pushClasses";
        K9_POLL_CLASSES = packageName + ".K9RemoteControl.pollClasses";
        K9_POLL_FREQUENCY = packageName + ".K9RemoteControl.pollFrequency";
        K9_BACKGROUND_OPERATIONS = packageName + ".K9RemoteControl.backgroundOperations";
        K9_THEME = packageName + ".K9RemoteControl.theme";
    }

    public static void set(Context context, Intent broadcastIntent) {
        broadcastIntent.setAction(K9RemoteControl.K9_SET);
        context.sendBroadcast(broadcastIntent, K9RemoteControl.K9_REMOTE_CONTROL_PERMISSION);
    }

    public static void fetchAccounts(Context context, K9AccountReceptor receptor) {
        Intent accountFetchIntent = new Intent();
        accountFetchIntent.setAction(K9RemoteControl.K9_REQUEST_ACCOUNTS);
        AccountReceiver receiver = new AccountReceiver(receptor);
        context.sendOrderedBroadcast(accountFetchIntent, K9RemoteControl.K9_REMOTE_CONTROL_PERMISSION, receiver, null, Activity.RESULT_OK, null, null);
    }

}


