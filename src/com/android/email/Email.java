
package com.android.email;

import java.io.File;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Config;
import android.util.Log;

import com.android.email.activity.MessageCompose;
import com.android.email.mail.internet.BinaryTempFileBody;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.service.BootReceiver;
import com.android.email.service.MailService;

public class Email extends Application {
    public static Application app = null;
    public static File tempDirectory;
    public static final String LOG_TAG = "k9";
    
    /**
     * Some log messages can be sent to a file, so that the logs
     * can be read using unprivileged access (eg. Terminal Emulator)
     * on the phone, without adb.  Set to null to disable
     */
    public static final String logFile = null;
    //public static final String logFile = "/sdcard/k9mail/debug.log";

    /**
     * If this is enabled there will be additional logging information sent to
     * Log.d, including protocol dumps.
     */
    public static boolean DEBUG = true;

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

    /**
     * The MIME type(s) of attachments we're willing to send. At the moment it is not possible
     * to open a chooser with a list of filter types, so the chooser is only opened with the first
     * item in the list. The entire list will be used to filter down attachments that are added
     * with Intent.ACTION_SEND.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_SEND_TYPES = new String[] {
        "*/*"
    };

    /**
     * The MIME type(s) of attachments we're willing to view.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[] {
        "image/*",
        "audio/*",
        "text/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to view.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[] {
    };

    /**
     * The MIME type(s) of attachments we're willing to download to SD.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
        "*/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to download to SD.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
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
    
    public static final int PUSH_WAKE_LOCK_TIMEOUT = 30000;


    /**
     * LED color used for the new email notitication
     */
    public static final int NOTIFICATION_LED_COLOR = 0xffff00ff;

    /**
     * Time the LED is on when blicking on new email notification
     */
    public static final int NOTIFICATION_LED_ON_TIME = 500; 

    /**
     * Time the LED is off when blicking on new email notification
     */
    public static final int NOTIFICATION_LED_OFF_TIME = 2000;
    
    public static final boolean NOTIFICATION_LED_WHILE_SYNCING = false;
    public static final int NOTIFICATION_LED_DIM_COLOR = 0x77770077;
    public static final int NOTIFICATION_LED_FAST_ON_TIME = 100;
    public static final int NOTIFICATION_LED_FAST_OFF_TIME = 100;
    
    public static final int NOTIFICATION_LED_SENDING_FAILURE_COLOR = 0xffff0000;

    // Must not conflict with an account number
    public static final int FETCHING_EMAIL_NOTIFICATION_ID      = -4; 
    public static final int FETCHING_EMAIL_NOTIFICATION_MULTI_ACCOUNT_ID      = -1;
    public static final int FETCHING_EMAIL_NOTIFICATION_NO_ACCOUNT = -2;
    public static final int CONNECTIVITY_ID = -3;
    
    // Backup formats in case they can't be fetched from the system
    public static final String BACKUP_DATE_FORMAT = "MM-dd-yyyy";
    public static final String TIME_FORMAT_12 = "h:mm a";
    public static final String TIME_FORMAT_24 = "H:mm";
    
    public static final int FLAGGED_COLOR = 0xff4444;

    public static final String INTENT_DATA_URI_SCHEMA           = "content";
    public static final String INTENT_DATA_UR_PATH_PREFIX       = "email";
    public static final String INTENT_DATA_URI_PREFIX           = INTENT_DATA_URI_SCHEMA + "://" + INTENT_DATA_UR_PATH_PREFIX;

    /**
     * Called throughout the application when the number of accounts has changed. This method
     * enables or disables the Compose activity, the boot receiver and the service based on
     * whether any accounts are configured.
     */
    public static void setServicesEnabled(Context context) {
        setServicesEnabled(context, Preferences.getPreferences(context).getAccounts().length > 0);
    }

    public static void setServicesEnabled(Context context, boolean enabled) {
        PackageManager pm = context.getPackageManager();
        if (!enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * If no accounts now exist but the service is still enabled we're about to disable it
             * so we'll reschedule to kill off any existing alarms.
             */
            MailService.actionReschedule(context);
        }
        pm.setComponentEnabledSetting(
                new ComponentName(context, MessageCompose.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(
                new ComponentName(context, BootReceiver.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(
                new ComponentName(context, MailService.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        if (enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * And now if accounts do exist then we've just enabled the service and we want to
             * schedule alarms for the new accounts.
             */
            MailService.actionReschedule(context);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        Preferences prefs = Preferences.getPreferences(this);
        DEBUG = prefs.getEnableDebugLogging();
        DEBUG_SENSITIVE = prefs.getEnableSensitiveLogging();
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

    }
}








