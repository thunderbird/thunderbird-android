
package com.fsck.k9;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Environment;

import com.fsck.k9.Account.SortType;
import com.fsck.k9.core.BuildConfig;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import timber.log.Timber;
import timber.log.Timber.DebugTree;


public class K9 {

    public static final int VERSION_MIGRATE_OPENPGP_TO_ACCOUNTS = 63;

    public static File tempDirectory;
    public static final String LOG_TAG = "k9";

    /**
     * Name of the {@link SharedPreferences} file used to store the last known version of the
     * accounts' databases.
     *
     * <p>
     * See {@code UpgradeDatabases} for a detailed explanation of the database upgrade process.
     * </p>
     */
    private static final String DATABASE_VERSION_CACHE = "database_version_cache";

    /**
     * Key used to store the last known database version of the accounts' databases.
     *
     * @see #DATABASE_VERSION_CACHE
     */
    private static final String KEY_LAST_ACCOUNT_DATABASE_VERSION = "last_account_database_version";

    public enum BACKGROUND_OPS {
        ALWAYS, NEVER, WHEN_CHECKED_AUTO_SYNC
    }

    private static String language = "";
    private static Theme theme = Theme.LIGHT;
    private static Theme messageViewTheme = Theme.USE_GLOBAL;
    private static Theme composerTheme = Theme.USE_GLOBAL;
    private static boolean useFixedMessageTheme = true;

    private static final FontSizes fontSizes = new FontSizes();

    private static BACKGROUND_OPS backgroundOps = BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC;
    /**
     * Some log messages can be sent to a file, so that the logs
     * can be read using unprivileged access (eg. Terminal Emulator)
     * on the phone, without adb.  Set to null to disable
     */
    public static final String logFile = null;
    //public static final String logFile = Environment.getExternalStorageDirectory() + "/k9mail/debug.log";

    /**
     * If this is enabled, various development settings will be enabled
     * It should NEVER be on for Market builds
     * Right now, it just governs strictmode
     **/
    public static boolean DEVELOPER_MODE = BuildConfig.DEBUG;


    /**
     * If this is enabled there will be additional logging information sent to
     * Log.d, including protocol dumps.
     * Controlled by Preferences at run-time
     */
    private static boolean DEBUG = false;

    /**
     * If this is enabled than logging that normally hides sensitive information
     * like passwords will show that information.
     */
    public static boolean DEBUG_SENSITIVE = false;

    /**
     * A reference to the {@link SharedPreferences} used for caching the last known database
     * version.
     *
     * @see #checkCachedDatabaseVersion(Context)
     * @see #setDatabasesUpToDate(boolean)
     */
    private static SharedPreferences databaseVersionCache;

    private static boolean animations = true;

    private static boolean confirmDelete = false;
    private static boolean confirmDiscardMessage = true;
    private static boolean confirmDeleteStarred = false;
    private static boolean confirmSpam = false;
    private static boolean confirmDeleteFromNotification = true;
    private static boolean confirmMarkAllRead = true;

    private static NotificationHideSubject notificationHideSubject = NotificationHideSubject.NEVER;

    /**
     * Controls when to hide the subject in the notification area.
     */
    public enum NotificationHideSubject {
        ALWAYS,
        WHEN_LOCKED,
        NEVER
    }

    private static NotificationQuickDelete notificationQuickDelete = NotificationQuickDelete.NEVER;

    /**
     * Controls behaviour of delete button in notifications.
     */
    public enum NotificationQuickDelete {
        ALWAYS,
        FOR_SINGLE_MSG,
        NEVER
    }

    private static LockScreenNotificationVisibility sLockScreenNotificationVisibility =
        LockScreenNotificationVisibility.MESSAGE_COUNT;

    public enum LockScreenNotificationVisibility {
        EVERYTHING,
        SENDERS,
        MESSAGE_COUNT,
        APP_NAME,
        NOTHING
    }

    /**
     * Controls when to use the message list split view.
     */
    public enum SplitViewMode {
        ALWAYS,
        NEVER,
        WHEN_IN_LANDSCAPE
    }

    private static boolean messageListCheckboxes = true;
    private static boolean messageListStars = true;
    private static int messageListPreviewLines = 2;

    private static boolean showCorrespondentNames = true;
    private static boolean messageListSenderAboveSubject = false;
    private static boolean showContactName = false;
    private static boolean changeContactNameColor = false;
    private static int contactNameColor = 0xff00008f;
    private static boolean showContactPicture = true;
    private static boolean messageViewFixedWidthFont = false;
    private static boolean messageViewReturnToList = false;
    private static boolean messageViewShowNext = false;

    private static boolean gesturesEnabled = true;
    private static boolean useVolumeKeysForNavigation = false;
    private static boolean useVolumeKeysForListNavigation = false;
    private static boolean startIntegratedInbox = false;
    private static boolean measureAccounts = true;
    private static boolean countSearchMessages = true;
    private static boolean hideSpecialAccounts = false;
    private static boolean autofitWidth;
    private static boolean quietTimeEnabled = false;
    private static boolean notificationDuringQuietTimeEnabled = true;
    private static String quietTimeStarts = null;
    private static String quietTimeEnds = null;
    private static String attachmentDefaultPath = "";
    private static boolean wrapFolderNames = false;
    private static boolean hideUserAgent = false;
    private static boolean hideTimeZone = false;
    private static boolean hideHostnameWhenConnecting = false;

    private static SortType sortType;
    private static Map<SortType, Boolean> sortAscending = new HashMap<>();

    private static boolean useBackgroundAsUnreadIndicator = true;
    private static boolean threadedViewEnabled = true;
    private static SplitViewMode splitViewMode = SplitViewMode.NEVER;
    private static boolean colorizeMissingContactPictures = true;

    private static boolean messageViewArchiveActionVisible = false;
    private static boolean messageViewDeleteActionVisible = true;
    private static boolean messageViewMoveActionVisible = false;
    private static boolean messageViewCopyActionVisible = false;
    private static boolean messageViewSpamActionVisible = false;

    private static int pgpInlineDialogCounter;
    private static int pgpSignOnlyDialogCounter;


    /**
     * @see #areDatabasesUpToDate()
     */
    private static boolean databasesUpToDate = false;

    public static final String LOCAL_UID_PREFIX = "K9LOCAL:";

    public static final String REMOTE_UID_PREFIX = "K9REMOTE:";

    public static final String IDENTITY_HEADER = K9MailLib.IDENTITY_HEADER;

    /**
     * Specifies how many messages will be shown in a folder by default. This number is set
     * on each new folder and can be incremented with "Load more messages..." by the
     * VISIBLE_LIMIT_INCREMENT
     */
    public static final int DEFAULT_VISIBLE_LIMIT = 25;

    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    public static final int MAX_ATTACHMENT_DOWNLOAD_SIZE = (128 * 1024 * 1024);


    /* How many times should K-9 try to deliver a message before giving up
     * until the app is killed and restarted
     */

    public static final int MAX_SEND_ATTEMPTS = 5;

    /**
     * Max time (in millis) the wake lock will be held for when background sync is happening
     */
    public static final int WAKE_LOCK_TIMEOUT = 600000;

    public static final int MANUAL_WAKE_LOCK_TIMEOUT = 120000;

    public static final int PUSH_WAKE_LOCK_TIMEOUT = K9MailLib.PUSH_WAKE_LOCK_TIMEOUT;

    public static final int MAIL_SERVICE_WAKE_LOCK_TIMEOUT = 60000;

    public static final int BOOT_RECEIVER_WAKE_LOCK_TIMEOUT = 60000;

    public static class Intents {
        public static class Share {
            public static String EXTRA_FROM;
        }

        static void init(String packageName) {
            Share.EXTRA_FROM = packageName + ".intent.extra.SENDER";
        }
    }

    public static void save(StorageEditor editor) {
        editor.putBoolean("enableDebugLogging", K9.DEBUG);
        editor.putBoolean("enableSensitiveLogging", K9.DEBUG_SENSITIVE);
        editor.putString("backgroundOperations", K9.backgroundOps.name());
        editor.putBoolean("animations", animations);
        editor.putBoolean("gesturesEnabled", gesturesEnabled);
        editor.putBoolean("useVolumeKeysForNavigation", useVolumeKeysForNavigation);
        editor.putBoolean("useVolumeKeysForListNavigation", useVolumeKeysForListNavigation);
        editor.putBoolean("autofitWidth", autofitWidth);
        editor.putBoolean("quietTimeEnabled", quietTimeEnabled);
        editor.putBoolean("notificationDuringQuietTimeEnabled", notificationDuringQuietTimeEnabled);
        editor.putString("quietTimeStarts", quietTimeStarts);
        editor.putString("quietTimeEnds", quietTimeEnds);

        editor.putBoolean("startIntegratedInbox", startIntegratedInbox);
        editor.putBoolean("measureAccounts", measureAccounts);
        editor.putBoolean("countSearchMessages", countSearchMessages);
        editor.putBoolean("messageListSenderAboveSubject", messageListSenderAboveSubject);
        editor.putBoolean("hideSpecialAccounts", hideSpecialAccounts);
        editor.putBoolean("messageListStars", messageListStars);
        editor.putInt("messageListPreviewLines", messageListPreviewLines);
        editor.putBoolean("messageListCheckboxes", messageListCheckboxes);
        editor.putBoolean("showCorrespondentNames", showCorrespondentNames);
        editor.putBoolean("showContactName", showContactName);
        editor.putBoolean("showContactPicture", showContactPicture);
        editor.putBoolean("changeRegisteredNameColor", changeContactNameColor);
        editor.putInt("registeredNameColor", contactNameColor);
        editor.putBoolean("messageViewFixedWidthFont", messageViewFixedWidthFont);
        editor.putBoolean("messageViewReturnToList", messageViewReturnToList);
        editor.putBoolean("messageViewShowNext", messageViewShowNext);
        editor.putBoolean("wrapFolderNames", wrapFolderNames);
        editor.putBoolean("hideUserAgent", hideUserAgent);
        editor.putBoolean("hideTimeZone", hideTimeZone);
        editor.putBoolean("hideHostnameWhenConnecting", hideHostnameWhenConnecting);

        editor.putString("language", language);
        editor.putInt("theme", theme.ordinal());
        editor.putInt("messageViewTheme", messageViewTheme.ordinal());
        editor.putInt("messageComposeTheme", composerTheme.ordinal());
        editor.putBoolean("fixedMessageViewTheme", useFixedMessageTheme);

        editor.putBoolean("confirmDelete", confirmDelete);
        editor.putBoolean("confirmDiscardMessage", confirmDiscardMessage);
        editor.putBoolean("confirmDeleteStarred", confirmDeleteStarred);
        editor.putBoolean("confirmSpam", confirmSpam);
        editor.putBoolean("confirmDeleteFromNotification", confirmDeleteFromNotification);
        editor.putBoolean("confirmMarkAllRead", confirmMarkAllRead);

        editor.putString("sortTypeEnum", sortType.name());
        editor.putBoolean("sortAscending", sortAscending.get(sortType));

        editor.putString("notificationHideSubject", notificationHideSubject.toString());
        editor.putString("notificationQuickDelete", notificationQuickDelete.toString());
        editor.putString("lockScreenNotificationVisibility", sLockScreenNotificationVisibility.toString());

        editor.putString("attachmentdefaultpath", attachmentDefaultPath);
        editor.putBoolean("useBackgroundAsUnreadIndicator", useBackgroundAsUnreadIndicator);
        editor.putBoolean("threadedView", threadedViewEnabled);
        editor.putString("splitViewMode", splitViewMode.name());
        editor.putBoolean("colorizeMissingContactPictures", colorizeMissingContactPictures);

        editor.putBoolean("messageViewArchiveActionVisible", messageViewArchiveActionVisible);
        editor.putBoolean("messageViewDeleteActionVisible", messageViewDeleteActionVisible);
        editor.putBoolean("messageViewMoveActionVisible", messageViewMoveActionVisible);
        editor.putBoolean("messageViewCopyActionVisible", messageViewCopyActionVisible);
        editor.putBoolean("messageViewSpamActionVisible", messageViewSpamActionVisible);

        editor.putInt("pgpInlineDialogCounter", pgpInlineDialogCounter);
        editor.putInt("pgpSignOnlyDialogCounter", pgpSignOnlyDialogCounter);

        fontSizes.save(editor);
    }

    public static void init(Context context) {
        K9MailLib.setDebugStatus(new K9MailLib.DebugStatus() {
            @Override public boolean enabled() {
                return DEBUG;
            }

            @Override public boolean debugSensitive() {
                return DEBUG_SENSITIVE;
            }
        });

        checkCachedDatabaseVersion(context);

        Preferences prefs = DI.get(Preferences.class);
        loadPrefs(prefs);
    }

    /**
     * Loads the last known database version of the accounts' databases from a
     * {@code SharedPreference}.
     *
     * <p>
     * If the stored version matches {@link LocalStore#getDbVersion()} we know that the databases are
     * up to date.<br>
     * Using {@code SharedPreferences} should be a lot faster than opening all SQLite databases to
     * get the current database version.
     * </p><p>
     * See the class {@code UpgradeDatabases} for a detailed explanation of the database upgrade process.
     * </p>
     *
     * @see #areDatabasesUpToDate()
     */
    public static void checkCachedDatabaseVersion(Context context) {
        databaseVersionCache = context.getSharedPreferences(DATABASE_VERSION_CACHE, Context.MODE_PRIVATE);

        int cachedVersion = databaseVersionCache.getInt(KEY_LAST_ACCOUNT_DATABASE_VERSION, 0);

        if (cachedVersion >= LocalStore.getDbVersion()) {
            K9.setDatabasesUpToDate(false);
        }
        if (cachedVersion < VERSION_MIGRATE_OPENPGP_TO_ACCOUNTS) {
            migrateOpenPgpGlobalToAccountSettings();
        }
    }

    private static void migrateOpenPgpGlobalToAccountSettings() {
        Preferences preferences = DI.get(Preferences.class);
        Storage storage = preferences.getStorage();

        String openPgpProvider = storage.getString("openPgpProvider", null);
        boolean openPgpSupportSignOnly = storage.getBoolean("openPgpSupportSignOnly", false);

        for (Account account : preferences.getAccounts()) {
            account.setOpenPgpProvider(openPgpProvider);
            account.setOpenPgpHideSignOnly(!openPgpSupportSignOnly);
            account.save();
        }

        storage.edit()
                .remove("openPgpProvider")
                .remove("openPgpSupportSignOnly")
                .commit();
    }

    /**
     * Load preferences into our statics.
     *
     * If you're adding a preference here, odds are you'll need to add it to
     * {@link com.fsck.k9.preferences.GlobalSettings}, too.
     *
     * @param prefs Preferences to load
     */
    public static void loadPrefs(Preferences prefs) {
        Storage storage = prefs.getStorage();
        setDebug(storage.getBoolean("enableDebugLogging", DEVELOPER_MODE));
        DEBUG_SENSITIVE = storage.getBoolean("enableSensitiveLogging", false);
        animations = storage.getBoolean("animations", true);
        gesturesEnabled = storage.getBoolean("gesturesEnabled", false);
        useVolumeKeysForNavigation = storage.getBoolean("useVolumeKeysForNavigation", false);
        useVolumeKeysForListNavigation = storage.getBoolean("useVolumeKeysForListNavigation", false);
        startIntegratedInbox = storage.getBoolean("startIntegratedInbox", false);
        measureAccounts = storage.getBoolean("measureAccounts", true);
        countSearchMessages = storage.getBoolean("countSearchMessages", true);
        hideSpecialAccounts = storage.getBoolean("hideSpecialAccounts", false);
        messageListSenderAboveSubject = storage.getBoolean("messageListSenderAboveSubject", false);
        messageListCheckboxes = storage.getBoolean("messageListCheckboxes", false);
        messageListStars = storage.getBoolean("messageListStars", true);
        messageListPreviewLines = storage.getInt("messageListPreviewLines", 2);

        autofitWidth = storage.getBoolean("autofitWidth", true);

        quietTimeEnabled = storage.getBoolean("quietTimeEnabled", false);
        notificationDuringQuietTimeEnabled = storage.getBoolean("notificationDuringQuietTimeEnabled", true);
        quietTimeStarts = storage.getString("quietTimeStarts", "21:00");
        quietTimeEnds = storage.getString("quietTimeEnds", "7:00");

        showCorrespondentNames = storage.getBoolean("showCorrespondentNames", true);
        showContactName = storage.getBoolean("showContactName", false);
        showContactPicture = storage.getBoolean("showContactPicture", true);
        changeContactNameColor = storage.getBoolean("changeRegisteredNameColor", false);
        contactNameColor = storage.getInt("registeredNameColor", 0xff00008f);
        messageViewFixedWidthFont = storage.getBoolean("messageViewFixedWidthFont", false);
        messageViewReturnToList = storage.getBoolean("messageViewReturnToList", false);
        messageViewShowNext = storage.getBoolean("messageViewShowNext", false);
        wrapFolderNames = storage.getBoolean("wrapFolderNames", false);
        hideUserAgent = storage.getBoolean("hideUserAgent", false);
        hideTimeZone = storage.getBoolean("hideTimeZone", false);
        hideHostnameWhenConnecting = storage.getBoolean("hideHostnameWhenConnecting", false);

        confirmDelete = storage.getBoolean("confirmDelete", false);
        confirmDiscardMessage = storage.getBoolean("confirmDiscardMessage", true);
        confirmDeleteStarred = storage.getBoolean("confirmDeleteStarred", false);
        confirmSpam = storage.getBoolean("confirmSpam", false);
        confirmDeleteFromNotification = storage.getBoolean("confirmDeleteFromNotification", true);
        confirmMarkAllRead = storage.getBoolean("confirmMarkAllRead", true);

        try {
            String value = storage.getString("sortTypeEnum", Account.DEFAULT_SORT_TYPE.name());
            sortType = SortType.valueOf(value);
        } catch (Exception e) {
            sortType = Account.DEFAULT_SORT_TYPE;
        }

        boolean sortAscending = storage.getBoolean("sortAscending", Account.DEFAULT_SORT_ASCENDING);
        K9.sortAscending.put(sortType, sortAscending);

        String notificationHideSubject = storage.getString("notificationHideSubject", null);
        if (notificationHideSubject == null) {
            // If the "notificationHideSubject" setting couldn't be found, the app was probably
            // updated. Look for the old "keyguardPrivacy" setting and map it to the new enum.
            K9.notificationHideSubject = (storage.getBoolean("keyguardPrivacy", false)) ?
                    NotificationHideSubject.WHEN_LOCKED : NotificationHideSubject.NEVER;
        } else {
            K9.notificationHideSubject = NotificationHideSubject.valueOf(notificationHideSubject);
        }

        String notificationQuickDelete = storage.getString("notificationQuickDelete", null);
        if (notificationQuickDelete != null) {
            K9.notificationQuickDelete = NotificationQuickDelete.valueOf(notificationQuickDelete);
        }

        String lockScreenNotificationVisibility = storage.getString("lockScreenNotificationVisibility", null);
        if(lockScreenNotificationVisibility != null) {
            sLockScreenNotificationVisibility = LockScreenNotificationVisibility.valueOf(lockScreenNotificationVisibility);
        }

        String splitViewMode = storage.getString("splitViewMode", null);
        if (splitViewMode != null) {
            K9.splitViewMode = SplitViewMode.valueOf(splitViewMode);
        }

        attachmentDefaultPath = storage.getString("attachmentdefaultpath",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        useBackgroundAsUnreadIndicator = storage.getBoolean("useBackgroundAsUnreadIndicator", true);
        threadedViewEnabled = storage.getBoolean("threadedView", true);
        fontSizes.load(storage);

        try {
            setBackgroundOps(BACKGROUND_OPS.valueOf(storage.getString(
                    "backgroundOperations",
                    BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC.name())));
        } catch (Exception e) {
            setBackgroundOps(BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC);
        }

        colorizeMissingContactPictures = storage.getBoolean("colorizeMissingContactPictures", true);

        messageViewArchiveActionVisible = storage.getBoolean("messageViewArchiveActionVisible", false);
        messageViewDeleteActionVisible = storage.getBoolean("messageViewDeleteActionVisible", true);
        messageViewMoveActionVisible = storage.getBoolean("messageViewMoveActionVisible", false);
        messageViewCopyActionVisible = storage.getBoolean("messageViewCopyActionVisible", false);
        messageViewSpamActionVisible = storage.getBoolean("messageViewSpamActionVisible", false);

        pgpInlineDialogCounter = storage.getInt("pgpInlineDialogCounter", 0);
        pgpSignOnlyDialogCounter = storage.getInt("pgpSignOnlyDialogCounter", 0);

        K9.setK9Language(storage.getString("language", ""));

        int themeValue = storage.getInt("theme", Theme.LIGHT.ordinal());
        // We used to save the resource ID of the theme. So convert that to the new format if
        // necessary.
        if (themeValue == Theme.DARK.ordinal() || themeValue == android.R.style.Theme) {
            K9.setK9Theme(Theme.DARK);
        } else {
            K9.setK9Theme(Theme.LIGHT);
        }

        themeValue = storage.getInt("messageViewTheme", Theme.USE_GLOBAL.ordinal());
        K9.setK9MessageViewThemeSetting(Theme.values()[themeValue]);
        themeValue = storage.getInt("messageComposeTheme", Theme.USE_GLOBAL.ordinal());
        K9.setK9ComposerThemeSetting(Theme.values()[themeValue]);
        K9.setUseFixedMessageViewTheme(storage.getBoolean("fixedMessageViewTheme", true));
    }

    public static String getK9Language() {
        return language;
    }

    public static void setK9Language(String nlanguage) {
        language = nlanguage;
    }

    /**
     * Possible values for the different theme settings.
     *
     * <p><strong>Important:</strong>
     * Do not change the order of the items! The ordinal value (position) is used when saving the
     * settings.</p>
     */
    public enum Theme {
        LIGHT,
        DARK,
        USE_GLOBAL
    }

    public static Theme getK9MessageViewTheme() {
        return messageViewTheme == Theme.USE_GLOBAL ? theme : messageViewTheme;
    }

    public static Theme getK9MessageViewThemeSetting() {
        return messageViewTheme;
    }

    public static Theme getK9ComposerTheme() {
        return composerTheme == Theme.USE_GLOBAL ? theme : composerTheme;
    }

    public static Theme getK9ComposerThemeSetting() {
        return composerTheme;
    }

    public static Theme getK9Theme() {
        return theme;
    }

    public static void setK9Theme(Theme ntheme) {
        if (ntheme != Theme.USE_GLOBAL) {
            theme = ntheme;
        }
    }

    public static void setK9MessageViewThemeSetting(Theme nMessageViewTheme) {
        messageViewTheme = nMessageViewTheme;
    }

    public static boolean useFixedMessageViewTheme() {
        return useFixedMessageTheme;
    }

    public static void setK9ComposerThemeSetting(Theme compTheme) {
        composerTheme = compTheme;
    }

    public static void setUseFixedMessageViewTheme(boolean useFixed) {
        useFixedMessageTheme = useFixed;
        if (!useFixedMessageTheme && messageViewTheme == Theme.USE_GLOBAL) {
            messageViewTheme = theme;
        }
    }

    public static BACKGROUND_OPS getBackgroundOps() {
        return backgroundOps;
    }

    public static boolean setBackgroundOps(BACKGROUND_OPS backgroundOps) {
        BACKGROUND_OPS oldBackgroundOps = K9.backgroundOps;
        K9.backgroundOps = backgroundOps;
        return backgroundOps != oldBackgroundOps;
    }

    public static boolean setBackgroundOps(String nbackgroundOps) {
        return setBackgroundOps(BACKGROUND_OPS.valueOf(nbackgroundOps));
    }

    public static boolean gesturesEnabled() {
        return gesturesEnabled;
    }

    public static void setGesturesEnabled(boolean gestures) {
        gesturesEnabled = gestures;
    }

    public static boolean useVolumeKeysForNavigationEnabled() {
        return useVolumeKeysForNavigation;
    }

    public static void setUseVolumeKeysForNavigation(boolean volume) {
        useVolumeKeysForNavigation = volume;
    }

    public static boolean useVolumeKeysForListNavigationEnabled() {
        return useVolumeKeysForListNavigation;
    }

    public static void setUseVolumeKeysForListNavigation(boolean enabled) {
        useVolumeKeysForListNavigation = enabled;
    }

    public static boolean autofitWidth() {
        return autofitWidth;
    }

    public static void setAutofitWidth(boolean autofitWidth) {
        K9.autofitWidth = autofitWidth;
    }

    public static boolean getQuietTimeEnabled() {
        return quietTimeEnabled;
    }

    public static void setQuietTimeEnabled(boolean quietTimeEnabled) {
        K9.quietTimeEnabled = quietTimeEnabled;
    }

    public static boolean isNotificationDuringQuietTimeEnabled() {
        return notificationDuringQuietTimeEnabled;
    }

    public static void setNotificationDuringQuietTimeEnabled(boolean notificationDuringQuietTimeEnabled) {
        K9.notificationDuringQuietTimeEnabled = notificationDuringQuietTimeEnabled;
    }

    public static String getQuietTimeStarts() {
        return quietTimeStarts;
    }

    public static void setQuietTimeStarts(String quietTimeStarts) {
        K9.quietTimeStarts = quietTimeStarts;
    }

    public static String getQuietTimeEnds() {
        return quietTimeEnds;
    }

    public static void setQuietTimeEnds(String quietTimeEnds) {
        K9.quietTimeEnds = quietTimeEnds;
    }


    public static boolean isQuietTime() {
        if (!quietTimeEnabled) {
            return false;
        }

        QuietTimeChecker quietTimeChecker = new QuietTimeChecker(Clock.INSTANCE, quietTimeStarts, quietTimeEnds);
        return quietTimeChecker.isQuietTime();
    }

    public static void setDebug(boolean debug) {
        K9.DEBUG = debug;
        updateLoggingStatus();
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static boolean startIntegratedInbox() {
        return startIntegratedInbox;
    }

    public static void setStartIntegratedInbox(boolean startIntegratedInbox) {
        K9.startIntegratedInbox = startIntegratedInbox;
    }

    public static boolean showAnimations() {
        return animations;
    }

    public static void setAnimations(boolean animations) {
        K9.animations = animations;
    }

    public static int messageListPreviewLines() {
        return messageListPreviewLines;
    }

    public static void setMessageListPreviewLines(int lines) {
        messageListPreviewLines = lines;
    }

    public static boolean messageListCheckboxes() {
        return messageListCheckboxes;
    }

    public static void setMessageListCheckboxes(boolean checkboxes) {
        messageListCheckboxes = checkboxes;
    }

    public static boolean messageListStars() {
        return messageListStars;
    }

    public static void setMessageListStars(boolean stars) {
        messageListStars = stars;
    }

    public static boolean showCorrespondentNames() {
        return showCorrespondentNames;
    }

     public static boolean messageListSenderAboveSubject() {
         return messageListSenderAboveSubject;
     }

    public static void setMessageListSenderAboveSubject(boolean sender) {
         messageListSenderAboveSubject = sender;
    }
    public static void setShowCorrespondentNames(boolean showCorrespondentNames) {
        K9.showCorrespondentNames = showCorrespondentNames;
    }

    public static boolean showContactName() {
        return showContactName;
    }

    public static void setShowContactName(boolean showContactName) {
        K9.showContactName = showContactName;
    }

    public static boolean changeContactNameColor() {
        return changeContactNameColor;
    }

    public static void setChangeContactNameColor(boolean changeContactNameColor) {
        K9.changeContactNameColor = changeContactNameColor;
    }

    public static int getContactNameColor() {
        return contactNameColor;
    }

    public static void setContactNameColor(int contactNameColor) {
        K9.contactNameColor = contactNameColor;
    }

    public static boolean messageViewFixedWidthFont() {
        return messageViewFixedWidthFont;
    }

    public static void setMessageViewFixedWidthFont(boolean fixed) {
        messageViewFixedWidthFont = fixed;
    }

    public static boolean messageViewReturnToList() {
        return messageViewReturnToList;
    }

    public static void setMessageViewReturnToList(boolean messageViewReturnToList) {
        K9.messageViewReturnToList = messageViewReturnToList;
    }

    public static boolean messageViewShowNext() {
        return messageViewShowNext;
    }

    public static void setMessageViewShowNext(boolean messageViewShowNext) {
        K9.messageViewShowNext = messageViewShowNext;
    }

    public static FontSizes getFontSizes() {
        return fontSizes;
    }

    public static boolean measureAccounts() {
        return measureAccounts;
    }

    public static void setMeasureAccounts(boolean measureAccounts) {
        K9.measureAccounts = measureAccounts;
    }

    public static boolean countSearchMessages() {
        return countSearchMessages;
    }

    public static void setCountSearchMessages(boolean countSearchMessages) {
        K9.countSearchMessages = countSearchMessages;
    }

    public static boolean isHideSpecialAccounts() {
        return hideSpecialAccounts;
    }

    public static void setHideSpecialAccounts(boolean hideSpecialAccounts) {
        K9.hideSpecialAccounts = hideSpecialAccounts;
    }

    public static boolean confirmDelete() {
        return confirmDelete;
    }

    public static void setConfirmDelete(final boolean confirm) {
        confirmDelete = confirm;
    }

    public static boolean confirmDeleteStarred() {
        return confirmDeleteStarred;
    }

    public static void setConfirmDeleteStarred(final boolean confirm) {
        confirmDeleteStarred = confirm;
    }

    public static boolean confirmSpam() {
        return confirmSpam;
    }

    public static boolean confirmDiscardMessage() {
        return confirmDiscardMessage;
    }

    public static void setConfirmSpam(final boolean confirm) {
        confirmSpam = confirm;
    }

    public static void setConfirmDiscardMessage(final boolean confirm) {
        confirmDiscardMessage = confirm;
    }

    public static boolean confirmDeleteFromNotification() {
        return confirmDeleteFromNotification;
    }

    public static void setConfirmDeleteFromNotification(final boolean confirm) {
        confirmDeleteFromNotification = confirm;
    }

    public static boolean confirmMarkAllRead() {
        return confirmMarkAllRead;
    }

    public static void setConfirmMarkAllRead(final boolean confirm) {
        confirmMarkAllRead = confirm;
    }

    public static NotificationHideSubject getNotificationHideSubject() {
        return notificationHideSubject;
    }

    public static void setNotificationHideSubject(final NotificationHideSubject mode) {
        notificationHideSubject = mode;
    }

    public static NotificationQuickDelete getNotificationQuickDeleteBehaviour() {
        return notificationQuickDelete;
    }

    public static void setNotificationQuickDeleteBehaviour(final NotificationQuickDelete mode) {
        notificationQuickDelete = mode;
    }

    public static LockScreenNotificationVisibility getLockScreenNotificationVisibility() {
        return sLockScreenNotificationVisibility;
    }

    public static void setLockScreenNotificationVisibility(final LockScreenNotificationVisibility visibility) {
        sLockScreenNotificationVisibility = visibility;
    }

    public static boolean wrapFolderNames() {
        return wrapFolderNames;
    }
    public static void setWrapFolderNames(final boolean state) {
        wrapFolderNames = state;
    }

    public static boolean hideUserAgent() {
        return hideUserAgent;
    }
    public static void setHideUserAgent(final boolean state) {
        hideUserAgent = state;
    }

    public static boolean hideTimeZone() {
        return hideTimeZone;
    }
    public static void setHideTimeZone(final boolean state) {
        hideTimeZone = state;
    }

    public static boolean hideHostnameWhenConnecting() {
        return hideHostnameWhenConnecting;
    }

    public static void setHideHostnameWhenConnecting(final boolean state) {
        hideHostnameWhenConnecting = state;
    }

    public static String getAttachmentDefaultPath() {
        return attachmentDefaultPath;
    }

    public static void setAttachmentDefaultPath(String attachmentDefaultPath) {
        K9.attachmentDefaultPath = attachmentDefaultPath;
    }

    public static synchronized SortType getSortType() {
        return sortType;
    }

    public static synchronized void setSortType(SortType sortType) {
        K9.sortType = sortType;
    }

    public static synchronized boolean isSortAscending(SortType sortType) {
        if (sortAscending.get(sortType) == null) {
            sortAscending.put(sortType, sortType.isDefaultAscending());
        }
        return sortAscending.get(sortType);
    }

    public static synchronized void setSortAscending(SortType sortType, boolean sortAscending) {
        K9.sortAscending.put(sortType, sortAscending);
    }

    public static synchronized boolean useBackgroundAsUnreadIndicator() {
        return useBackgroundAsUnreadIndicator;
    }

    public static synchronized void setUseBackgroundAsUnreadIndicator(boolean enabled) {
        useBackgroundAsUnreadIndicator = enabled;
    }

    public static synchronized boolean isThreadedViewEnabled() {
        return threadedViewEnabled;
    }

    public static synchronized void setThreadedViewEnabled(boolean enable) {
        threadedViewEnabled = enable;
    }

    public static synchronized SplitViewMode getSplitViewMode() {
        return splitViewMode;
    }

    public static synchronized void setSplitViewMode(SplitViewMode mode) {
        splitViewMode = mode;
    }

    public static boolean showContactPicture() {
        return showContactPicture;
    }

    public static void setShowContactPicture(boolean show) {
        showContactPicture = show;
    }

    public static boolean isColorizeMissingContactPictures() {
        return colorizeMissingContactPictures;
    }

    public static void setColorizeMissingContactPictures(boolean enabled) {
        colorizeMissingContactPictures = enabled;
    }

    public static boolean isMessageViewArchiveActionVisible() {
        return messageViewArchiveActionVisible;
    }

    public static void setMessageViewArchiveActionVisible(boolean visible) {
        messageViewArchiveActionVisible = visible;
    }

    public static boolean isMessageViewDeleteActionVisible() {
        return messageViewDeleteActionVisible;
    }

    public static void setMessageViewDeleteActionVisible(boolean visible) {
        messageViewDeleteActionVisible = visible;
    }

    public static boolean isMessageViewMoveActionVisible() {
        return messageViewMoveActionVisible;
    }

    public static void setMessageViewMoveActionVisible(boolean visible) {
        messageViewMoveActionVisible = visible;
    }

    public static boolean isMessageViewCopyActionVisible() {
        return messageViewCopyActionVisible;
    }

    public static void setMessageViewCopyActionVisible(boolean visible) {
        messageViewCopyActionVisible = visible;
    }

    public static boolean isMessageViewSpamActionVisible() {
        return messageViewSpamActionVisible;
    }

    public static void setMessageViewSpamActionVisible(boolean visible) {
        messageViewSpamActionVisible = visible;
    }

    public static int getPgpInlineDialogCounter() {
        return pgpInlineDialogCounter;
    }

    public static void setPgpInlineDialogCounter(int pgpInlineDialogCounter) {
        K9.pgpInlineDialogCounter = pgpInlineDialogCounter;
    }

    public static int getPgpSignOnlyDialogCounter() {
        return pgpSignOnlyDialogCounter;
    }

    public static void setPgpSignOnlyDialogCounter(int pgpSignOnlyDialogCounter) {
        K9.pgpSignOnlyDialogCounter = pgpSignOnlyDialogCounter;
    }

    /**
     * Check if we already know whether all databases are using the current database schema.
     *
     * <p>
     * This method is only used for optimizations. If it returns {@code true} we can be certain that
     * getting a {@link LocalStore} instance won't trigger a schema upgrade.
     * </p>
     *
     * @return {@code true}, if we know that all databases are using the current database schema.
     *         {@code false}, otherwise.
     */
    public static synchronized boolean areDatabasesUpToDate() {
        return databasesUpToDate;
    }

    /**
     * Remember that all account databases are using the most recent database schema.
     *
     * @param save
     *         Whether or not to write the current database version to the
     *         {@code SharedPreferences} {@link #DATABASE_VERSION_CACHE}.
     *
     * @see #areDatabasesUpToDate()
     */
    public static synchronized void setDatabasesUpToDate(boolean save) {
        databasesUpToDate = true;

        if (save) {
            Editor editor = databaseVersionCache.edit();
            editor.putInt(KEY_LAST_ACCOUNT_DATABASE_VERSION, LocalStore.getDbVersion());
            editor.apply();
        }
    }

    private static void updateLoggingStatus() {
        Timber.uprootAll();
        boolean enableDebugLogging = BuildConfig.DEBUG || DEBUG;
        if (enableDebugLogging) {
            Timber.plant(new DebugTree());
        }
    }

    public static void saveSettingsAsync() {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Preferences prefs = DI.get(Preferences.class);
                StorageEditor editor = prefs.getStorage().edit();
                save(editor);
                editor.commit();

                return null;
            }
        }.execute();
    }

}
