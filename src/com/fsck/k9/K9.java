
package com.fsck.k9;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebSettings;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.service.BootReceiver;
import com.fsck.k9.service.MailService;

import java.io.File;
import java.lang.reflect.Method;

public class K9 extends Application
{
    public static Application app = null;
    public static File tempDirectory;
    public static final String LOG_TAG = "k9";

    public enum BACKGROUND_OPS
    {
        WHEN_CHECKED, ALWAYS, NEVER, WHEN_CHECKED_AUTO_SYNC
    }

    private static String language = "";
    private static int theme = android.R.style.Theme_Light;

    private static final FontSizes fontSizes = new FontSizes();

    private static BACKGROUND_OPS backgroundOps = BACKGROUND_OPS.WHEN_CHECKED;
    /**
     * Some log messages can be sent to a file, so that the logs
     * can be read using unprivileged access (eg. Terminal Emulator)
     * on the phone, without adb.  Set to null to disable
     */
    public static final String logFile = null;
    //public static final String logFile = Environment.getExternalStorageDirectory() + "/k9mail/debug.log";

    /**
     * If this is enabled there will be additional logging information sent to
     * Log.d, including protocol dumps.
     * Controlled by Preferences at run-time
     */
    public static boolean DEBUG = false;

    /**
     * If this is enabled than logging that normally hides sensitive information
     * like passwords will show that information.
     */
    public static boolean DEBUG_SENSITIVE = false;

    /**
     * Can create messages containing stack traces that can be forwarded
     * to the development team.
     */
    public static boolean ENABLE_ERROR_FOLDER = true;
    public static String ERROR_FOLDER_NAME = "K9mail-errors";


    private static boolean mAnimations = true;


    private static boolean mMessageListStars = true;
    private static boolean mMessageListCheckboxes = false;
    private static boolean mMessageListTouchable = false;

    private static boolean mMessageViewFixedWidthFont = false;
    private static boolean mMessageViewReturnToList = false;

    private static boolean mGesturesEnabled = true;
    private static boolean mManageBack = false;
    private static boolean mStartIntegratedInbox = false;
    private static boolean mMeasureAccounts = true;
    private static boolean mCountSearchMessages = true;

    private static boolean useGalleryBugWorkaround = false;
    private static boolean galleryBuggy;

    /**
     * We use WebSettings.getBlockNetworkLoads() to prevent the WebView that displays email
     * bodies from loading external resources over the network. Unfortunately this method
     * isn't exposed via the official Android API. That's why we use reflection to be able
     * to call the method.
     */
    private static final Method mGetBlockNetworkLoads = getMethod(WebSettings.class, "setBlockNetworkLoads");


    /**
     * The MIME type(s) of attachments we're willing to send. At the moment it is not possible
     * to open a chooser with a list of filter types, so the chooser is only opened with the first
     * item in the list. The entire list will be used to filter down attachments that are added
     * with Intent.ACTION_SEND.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_SEND_TYPES = new String[]
    {
        "*/*"
    };

    /**
     * The MIME type(s) of attachments we're willing to view.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[]
    {
        "*/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to view.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[]
    {
    };

    /**
     * The MIME type(s) of attachments we're willing to download to SD.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[]
    {
        "*/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to download to SD.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[]
    {
    };

    /**
     * The special name "INBOX" is used throughout the application to mean "Whatever folder
     * the server refers to as the user's Inbox. Placed here to ease use.
     */
    public static final String INBOX = "INBOX";

    /**
     * For use when displaying that no folder is selected
     */
    public static final String FOLDER_NONE = "-NONE-";

    public static final String LOCAL_UID_PREFIX = "K9LOCAL:";

    public static final String REMOTE_UID_PREFIX = "K9REMOTE:";

    public static final String K9MAIL_IDENTITY = "X-K9mail-Identity";

    /**
     * Specifies how many messages will be shown in a folder by default. This number is set
     * on each new folder and can be incremented with "Load more messages..." by the
     * VISIBLE_LIMIT_INCREMENT
     */
    public static int DEFAULT_VISIBLE_LIMIT = 25;

    /**
     * Number of additioanl messages to load when a user selectes "Load more messages..."
     */
    public static int VISIBLE_LIMIT_INCREMENT = 25;

    public static int MAX_SEND_ATTEMPTS = 5;

    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    public static final int MAX_ATTACHMENT_DOWNLOAD_SIZE = (5 * 1024 * 1024);

    /**
     * Max time (in millis) the wake lock will be held for when background sync is happening
     */
    public static final int WAKE_LOCK_TIMEOUT = 600000;

    public static final int MANUAL_WAKE_LOCK_TIMEOUT = 120000;

    public static final int PUSH_WAKE_LOCK_TIMEOUT = 60000;

    public static final int MAIL_SERVICE_WAKE_LOCK_TIMEOUT = 30000;

    public static final int BOOT_RECEIVER_WAKE_LOCK_TIMEOUT = 60000;

    /**
     * Time the LED is on when blinking on new email notification
     */
    public static final int NOTIFICATION_LED_ON_TIME = 500;

    /**
     * Time the LED is off when blicking on new email notification
     */
    public static final int NOTIFICATION_LED_OFF_TIME = 2000;

    public static final boolean NOTIFICATION_LED_WHILE_SYNCING = false;
    public static final int NOTIFICATION_LED_FAST_ON_TIME = 100;
    public static final int NOTIFICATION_LED_FAST_OFF_TIME = 100;

    public static final int NOTIFICATION_LED_SENDING_FAILURE_COLOR = 0xffff0000;

    // Must not conflict with an account number
    public static final int FETCHING_EMAIL_NOTIFICATION      = -5000;
    public static final int CONNECTIVITY_ID = -3;


    public class Intents
    {

        public class EmailReceived
        {
            public static final String ACTION_EMAIL_RECEIVED    = "com.fsck.k9.intent.action.EMAIL_RECEIVED";
            public static final String ACTION_EMAIL_DELETED     = "com.fsck.k9.intent.action.EMAIL_DELETED";
            public static final String EXTRA_ACCOUNT            = "com.fsck.k9.intent.extra.ACCOUNT";
            public static final String EXTRA_FOLDER             = "com.fsck.k9.intent.extra.FOLDER";
            public static final String EXTRA_SENT_DATE          = "com.fsck.k9.intent.extra.SENT_DATE";
            public static final String EXTRA_FROM               = "com.fsck.k9.intent.extra.FROM";
            public static final String EXTRA_TO                 = "com.fsck.k9.intent.extra.TO";
            public static final String EXTRA_CC                 = "com.fsck.k9.intent.extra.CC";
            public static final String EXTRA_BCC                = "com.fsck.k9.intent.extra.BCC";
            public static final String EXTRA_SUBJECT            = "com.fsck.k9.intent.extra.SUBJECT";
            public static final String EXTRA_FROM_SELF          = "com.fsck.k9.intent.extra.FROM_SELF";
        }

    }

    /**
     * Called throughout the application when the number of accounts has changed. This method
     * enables or disables the Compose activity, the boot receiver and the service based on
     * whether any accounts are configured.
     */
    public static void setServicesEnabled(Context context)
    {
        int acctLength = Preferences.getPreferences(context).getAccounts().length;

        setServicesEnabled(context, acctLength > 0, null);

    }

    public static void setServicesEnabled(Context context, Integer wakeLockId)
    {
        setServicesEnabled(context, Preferences.getPreferences(context).getAccounts().length > 0, wakeLockId);
    }

    public static void setServicesEnabled(Context context, boolean enabled, Integer wakeLockId)
    {

        PackageManager pm = context.getPackageManager();

        if (!enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        {
            /*
             * If no accounts now exist but the service is still enabled we're about to disable it
             * so we'll reschedule to kill off any existing alarms.
             */
            MailService.actionReset(context, wakeLockId);
        }
        Class<?>[] classes = { MessageCompose.class, BootReceiver.class, MailService.class };

        for (Class<?> clazz : classes)
        {

            boolean alreadyEnabled = pm.getComponentEnabledSetting(new ComponentName(context, clazz)) ==
                                     PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

            if (enabled != alreadyEnabled)
            {
                pm.setComponentEnabledSetting(
                    new ComponentName(context, clazz),
                    enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            }
        }

        if (enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        {
            /*
             * And now if accounts do exist then we've just enabled the service and we want to
             * schedule alarms for the new accounts.
             */
            MailService.actionReset(context, wakeLockId);
        }

    }

    public static void save(SharedPreferences.Editor editor)
    {
        editor.putBoolean("enableDebugLogging", K9.DEBUG);
        editor.putBoolean("enableSensitiveLogging", K9.DEBUG_SENSITIVE);
        editor.putString("backgroundOperations", K9.backgroundOps.toString());
        editor.putBoolean("animations", mAnimations);
        editor.putBoolean("gesturesEnabled", mGesturesEnabled);
        editor.putBoolean("manageBack", mManageBack);
        editor.putBoolean("startIntegratedInbox", mStartIntegratedInbox);
        editor.putBoolean("measureAccounts", mMeasureAccounts);
        editor.putBoolean("countSearchMessages", mCountSearchMessages);
        editor.putBoolean("messageListStars",mMessageListStars);
        editor.putBoolean("messageListCheckboxes",mMessageListCheckboxes);
        editor.putBoolean("messageListTouchable",mMessageListTouchable);

        editor.putBoolean("messageViewFixedWidthFont",mMessageViewFixedWidthFont);
        editor.putBoolean("messageViewReturnToList", mMessageViewReturnToList);

        editor.putString("language", language);
        editor.putInt("theme", theme);
        editor.putBoolean("useGalleryBugWorkaround", useGalleryBugWorkaround);

        fontSizes.save(editor);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        app = this;

        galleryBuggy = checkForBuggyGallery();

        Preferences prefs = Preferences.getPreferences(this);
        SharedPreferences sprefs = prefs.getPreferences();
        DEBUG = sprefs.getBoolean("enableDebugLogging", false);
        DEBUG_SENSITIVE = sprefs.getBoolean("enableSensitiveLogging", false);
        mAnimations = sprefs.getBoolean("animations", true);
        mGesturesEnabled = sprefs.getBoolean("gesturesEnabled", true);
        mManageBack = sprefs.getBoolean("manageBack", false);
        mStartIntegratedInbox = sprefs.getBoolean("startIntegratedInbox", false);
        mMeasureAccounts = sprefs.getBoolean("measureAccounts", true);
        mCountSearchMessages = sprefs.getBoolean("countSearchMessages", true);
        mMessageListStars = sprefs.getBoolean("messageListStars",true);
        mMessageListCheckboxes = sprefs.getBoolean("messageListCheckboxes",false);
        mMessageListTouchable = sprefs.getBoolean("messageListTouchable",false);

        mMessageViewFixedWidthFont = sprefs.getBoolean("messageViewFixedWidthFont", false);
        mMessageViewReturnToList = sprefs.getBoolean("messageViewReturnToList", false);

        useGalleryBugWorkaround = sprefs.getBoolean("useGalleryBugWorkaround", K9.isGalleryBuggy());

        fontSizes.load(sprefs);

        try
        {
            setBackgroundOps(BACKGROUND_OPS.valueOf(sprefs.getString("backgroundOperations", "WHEN_CHECKED")));
        }
        catch (Exception e)
        {
            setBackgroundOps(BACKGROUND_OPS.WHEN_CHECKED);
        }

        K9.setK9Language(sprefs.getString("language", ""));
        K9.setK9Theme(sprefs.getInt("theme", android.R.style.Theme_Light));
        MessagingController.getInstance(this).resetVisibleLimits(prefs.getAccounts());

        /*
         * We have to give MimeMessage a temp directory because File.createTempFile(String, String)
         * doesn't work in Android and MimeMessage does not have access to a Context.
         */
        BinaryTempFileBody.setTempDirectory(getCacheDir());

        /*
         * Enable background sync of messages
         */

        setServicesEnabled(this);

        MessagingController.getInstance(this).addListener(new MessagingListener()
        {
            private void broadcastIntent(String action, Account account, String folder, Message message)
            {
                try
                {
                    Uri uri = Uri.parse("email://messages/" + account.getAccountNumber() + "/" + Uri.encode(folder) + "/" + Uri.encode(message.getUid()));
                    Intent intent = new Intent(action, uri);
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_ACCOUNT, account.getDescription());
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_FOLDER, folder);
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_SENT_DATE, message.getSentDate());
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_FROM, Address.toString(message.getFrom()));
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_TO, Address.toString(message.getRecipients(Message.RecipientType.TO)));
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_CC, Address.toString(message.getRecipients(Message.RecipientType.CC)));
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_BCC, Address.toString(message.getRecipients(Message.RecipientType.BCC)));
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_SUBJECT, message.getSubject());
                    intent.putExtra(K9.Intents.EmailReceived.EXTRA_FROM_SELF, account.isAnIdentity(message.getFrom()));
                    K9.this.sendBroadcast(intent);
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Broadcasted: action=" + action
                              + " account=" + account.getDescription()
                              + " folder=" + folder
                              + " message uid=" + message.getUid()
                             );

                }
                catch (MessagingException e)
                {
                    Log.w(K9.LOG_TAG, "Error: action=" + action
                          + " account=" + account.getDescription()
                          + " folder=" + folder
                          + " message uid=" + message.getUid()
                         );
                }
            }

            @Override
            public void synchronizeMailboxRemovedMessage(Account account, String folder, Message message)
            {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_DELETED, account, folder, message);
            }

            @Override
            public void messageDeleted(Account account, String folder, Message message)
            {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_DELETED, account, folder, message);
            }

            @Override
            public void synchronizeMailboxNewMessage(Account account, String folder, Message message)
            {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_RECEIVED, account, folder, message);
            }


        });

    }

    public static String getK9Language()
    {
        return language;
    }

    public static void setK9Language(String nlanguage)
    {
        language = nlanguage;
    }

    public static int getK9Theme()
    {
        return theme;
    }

    public static void setK9Theme(int ntheme)
    {
        theme = ntheme;
    }

    public static BACKGROUND_OPS getBackgroundOps()
    {
        return backgroundOps;
    }

    public static boolean setBackgroundOps(BACKGROUND_OPS backgroundOps)
    {
        BACKGROUND_OPS oldBackgroundOps = K9.backgroundOps;
        K9.backgroundOps = backgroundOps;
        return backgroundOps != oldBackgroundOps;
    }

    public static boolean setBackgroundOps(String nbackgroundOps)
    {
        return setBackgroundOps(BACKGROUND_OPS.valueOf(nbackgroundOps));
    }

    public static boolean gesturesEnabled()
    {
        return mGesturesEnabled;
    }

    public static void setGesturesEnabled(boolean gestures)
    {
        mGesturesEnabled = gestures;
    }


    public static boolean manageBack()
    {
        return mManageBack;
    }

    public static void setManageBack(boolean manageBack)
    {
        mManageBack = manageBack;
    }

    public static boolean startIntegratedInbox()
    {
        return mStartIntegratedInbox;
    }

    public static void setStartIntegratedInbox(boolean startIntegratedInbox)
    {
        mStartIntegratedInbox = startIntegratedInbox;
    }

    public static boolean isAnimations()
    {
        return mAnimations;
    }

    public static void setAnimations(boolean animations)
    {
        mAnimations = animations;
    }

    public static boolean messageListTouchable()
    {
        return mMessageListTouchable;
    }

    public static void setMessageListTouchable(boolean touchy)
    {
        mMessageListTouchable = touchy;
    }

    public static boolean messageListStars()
    {
        return mMessageListStars;
    }

    public static void setMessageListStars(boolean stars)
    {
        mMessageListStars = stars;
    }
    public static boolean messageListCheckboxes()
    {
        return mMessageListCheckboxes;
    }

    public static void setMessageListCheckboxes(boolean checkboxes)
    {
        mMessageListCheckboxes = checkboxes;
    }


    public static boolean messageViewFixedWidthFont()
    {
        return mMessageViewFixedWidthFont;
    }

    public static void setMessageViewFixedWidthFont(boolean fixed)
    {
        mMessageViewFixedWidthFont = fixed;
    }

    public static boolean messageViewReturnToList()
    {
        return mMessageViewReturnToList;
    }

    public static void setMessageViewReturnToList(boolean messageViewReturnToList)
    {
        mMessageViewReturnToList = messageViewReturnToList;
    }

    private static Method getMethod(Class<?> classObject, String methodName)
    {
        try
        {
            Method method = classObject.getMethod(methodName, boolean.class);
            return method;
        }
        catch (NoSuchMethodException e)
        {
            Log.i(K9.LOG_TAG, "Can't get method " +
                  classObject.toString() + "." + methodName);
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Error while using reflection to get method " +
                  classObject.toString() + "." + methodName, e);
        }
        return null;
    }

    public static void setBlockNetworkLoads(WebSettings webSettings, boolean state)
    {
        if (mGetBlockNetworkLoads != null)
        {
            try
            {
                mGetBlockNetworkLoads.invoke(webSettings, state);
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Error on invoking WebSettings.setBlockNetworkLoads()", e);
            }
        }
    }

    public static FontSizes getFontSizes()
    {
        return fontSizes;
    }

    public static boolean measureAccounts()
    {
        return mMeasureAccounts;
    }

    public static void setMeasureAccounts(boolean measureAccounts)
    {
        mMeasureAccounts = measureAccounts;
    }

    public static boolean countSearchMessages()
    {
        return mCountSearchMessages;
    }

    public static void setCountSearchMessages(boolean countSearchMessages)
    {
        mCountSearchMessages = countSearchMessages;
    }

    public static boolean useGalleryBugWorkaround()
    {
        return useGalleryBugWorkaround;
    }

    public static void setUseGalleryBugWorkaround(boolean useGalleryBugWorkaround)
    {
        K9.useGalleryBugWorkaround = useGalleryBugWorkaround;
    }

    public static boolean isGalleryBuggy()
    {
        return galleryBuggy;
    }

    /**
     * Check if this system contains a buggy Gallery 3D package.
     *
     * We have to work around the fact that those Gallery versions won't show
     * any images or videos when the pick intent is used with a MIME type other
     * than image/* or video/*. See issue 1186.
     *
     * @return true, if a buggy Gallery 3D package was found. False, otherwise.
     */
    private boolean checkForBuggyGallery()
    {
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo("com.cooliris.media", 0);

            return (pi.versionCode == 30682);
        }
        catch (NameNotFoundException e)
        {
            return false;
        }
    }
}
