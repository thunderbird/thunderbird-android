
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
    public static final String LOG_TAG = "Email";

    public static File tempDirectory;

    /**
     * If this is enabled there will be additional logging information sent to
     * Log.d, including protocol dumps.
     */
    public static boolean DEBUG = false;

    /**
     * If this is enabled than logging that normally hides sensitive information
     * like passwords will show that information.
     */
    public static boolean DEBUG_SENSITIVE = false;


    /**
     * The MIME type(s) of attachments we're willing to send. At the moment it is not possible
     * to open a chooser with a list of filter types, so the chooser is only opened with the first
     * item in the list. The entire list will be used to filter down attachments that are added
     * with Intent.ACTION_SEND.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_SEND_TYPES = new String[] {
        "image/*",
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
        "image/gif",
    };

    /**
     * The MIME type(s) of attachments we're willing to download to SD.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
        "image/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to download to SD.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
        "image/gif",
    };

    /**
     * The special name "INBOX" is used throughout the application to mean "Whatever folder
     * the server refers to as the user's Inbox. Placed here to ease use.
     */
    public static final String INBOX = "INBOX";

    /**
     * Specifies how many messages will be shown in a folder by default. This number is set
     * on each new folder and can be incremented with "Load more messages..." by the
     * VISIBLE_LIMIT_INCREMENT
     */
    public static final int DEFAULT_VISIBLE_LIMIT = 25;

    /**
     * Number of additioanl messages to load when a user selectes "Load more messages..."
     */
    public static final int VISIBLE_LIMIT_INCREMENT = 25;

    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    public static final int MAX_ATTACHMENT_DOWNLOAD_SIZE = (5 * 1024 * 1024);

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
        Preferences prefs = Preferences.getPreferences(this);
        DEBUG = prefs.geteEnableDebugLogging();
        DEBUG_SENSITIVE = prefs.getEnableSensitiveLogging();
        MessagingController.getInstance(this).resetVisibleLimits(prefs.getAccounts());

        /*
         * We have to give MimeMessage a temp directory because File.createTempFile(String, String)
         * doesn't work in Android and MimeMessage does not have access to a Context.
         */
        BinaryTempFileBody.setTempDirectory(getCacheDir());
    }
}








