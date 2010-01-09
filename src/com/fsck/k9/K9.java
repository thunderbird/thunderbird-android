
package com.fsck.k9;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.service.BootReceiver;
import com.fsck.k9.service.MailService;

import java.io.File;
import java.util.UUID;

public class K9 extends Application
{
    public static Application app = null;
    public static File tempDirectory;
    public static final String LOG_TAG = "k9";

    public enum BACKGROUND_OPS
    {
        WHEN_CHECKED, ALWAYS, NEVER
    }

    private static int theme = android.R.style.Theme_Light;

    private static BACKGROUND_OPS backgroundOps = BACKGROUND_OPS.WHEN_CHECKED;
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


    private static boolean mMessageListLefthandedWidgets = false;


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

    /*
     * http://www.w3schools.com/media/media_mimeref.asp
     * + png
     */
    public static final String[][] CONTENT_TYPE_BY_EXTENSION_MAP = new String[][]
    {
        { "",       "application/octet-stream" },
        { "323",    "text/h323"},
        { "acx",    "application/internet-property-stream"},
        { "ai",     "application/postscript"},
        { "aif",    "audio/x-aiff"},
        { "aifc",   "audio/x-aiff"},
        { "aiff",   "audio/x-aiff"},
        { "asf",    "video/x-ms-asf"},
        { "asr",    "video/x-ms-asf"},
        { "asx",    "video/x-ms-asf"},
        { "au",     "audio/basic"},
        { "avi",    "video/x-msvideo"},
        { "axs",    "application/olescript"},
        { "bas",    "text/plain"},
        { "bcpio",  "application/x-bcpio"},
        { "bin",    "application/octet-stream"},
        { "bmp",    "image/bmp"},
        { "c",      "text/plain"},
        { "cat",    "application/vnd.ms-pkiseccat"},
        { "cdf",    "application/x-cdf"},
        { "cer",    "application/x-x509-ca-cert"},
        { "class",  "application/octet-stream"},
        { "clp",    "application/x-msclip"},
        { "cmx",    "image/x-cmx"},
        { "cod",    "image/cis-cod"},
        { "cpio",   "application/x-cpio"},
        { "crd",    "application/x-mscardfile"},
        { "crl",    "application/pkix-crl"},
        { "crt",    "application/x-x509-ca-cert"},
        { "csh",    "application/x-csh"},
        { "css",    "text/css"},
        { "dcr",    "application/x-director"},
        { "der",    "application/x-x509-ca-cert"},
        { "dir",    "application/x-director"},
        { "dll",    "application/x-msdownload"},
        { "dms",    "application/octet-stream"},
        { "doc",    "application/msword"},
        { "dot",    "application/msword"},
        { "dvi",    "application/x-dvi"},
        { "dxr",    "application/x-director"},
        { "eps",    "application/postscript"},
        { "etx",    "text/x-setext"},
        { "evy",    "application/envoy"},
        { "exe",    "application/octet-stream"},
        { "fif",    "application/fractals"},
        { "flr",    "x-world/x-vrml"},
        { "gif",    "image/gif"},
        { "gtar",   "application/x-gtar"},
        { "gz",     "application/x-gzip"},
        { "h",      "text/plain"},
        { "hdf",    "application/x-hdf"},
        { "hlp",    "application/winhlp"},
        { "hqx",    "application/mac-binhex40"},
        { "hta",    "application/hta"},
        { "htc",    "text/x-component"},
        { "htm",    "text/html"},
        { "html",   "text/html"},
        { "htt",    "text/webviewhtml"},
        { "ico",    "image/x-icon"},
        { "ief",    "image/ief"},
        { "iii",    "application/x-iphone"},
        { "ins",    "application/x-internet-signup"},
        { "isp",    "application/x-internet-signup"},
        { "jfif",   "image/pipeg"},
        { "jpe",    "image/jpeg"},
        { "jpeg",   "image/jpeg"},
        { "jpg",    "image/jpeg"},
        { "js",     "application/x-javascript"},
        { "latex",  "application/x-latex"},
        { "lha",    "application/octet-stream"},
        { "lsf",    "video/x-la-asf"},
        { "lsx",    "video/x-la-asf"},
        { "lzh",    "application/octet-stream"},
        { "m13",    "application/x-msmediaview"},
        { "m14",    "application/x-msmediaview"},
        { "m3u",    "audio/x-mpegurl"},
        { "man",    "application/x-troff-man"},
        { "mdb",    "application/x-msaccess"},
        { "me",     "application/x-troff-me"},
        { "mht",    "message/rfc822"},
        { "mhtml",  "message/rfc822"},
        { "mid",    "audio/mid"},
        { "mny",    "application/x-msmoney"},
        { "mov",    "video/quicktime"},
        { "movie",  "video/x-sgi-movie"},
        { "mp2",    "video/mpeg"},
        { "mp3",    "audio/mpeg"},
        { "mpa",    "video/mpeg"},
        { "mpe",    "video/mpeg"},
        { "mpeg",   "video/mpeg"},
        { "mpg",    "video/mpeg"},
        { "mpp",    "application/vnd.ms-project"},
        { "mpv2",   "video/mpeg"},
        { "ms",     "application/x-troff-ms"},
        { "mvb",    "application/x-msmediaview"},
        { "nws",    "message/rfc822"},
        { "oda",    "application/oda"},
        { "p10",    "application/pkcs10"},
        { "p12",    "application/x-pkcs12"},
        { "p7b",    "application/x-pkcs7-certificates"},
        { "p7c",    "application/x-pkcs7-mime"},
        { "p7m",    "application/x-pkcs7-mime"},
        { "p7r",    "application/x-pkcs7-certreqresp"},
        { "p7s",    "application/x-pkcs7-signature"},
        { "pbm",    "image/x-portable-bitmap"},
        { "pdf",    "application/pdf"},
        { "pfx",    "application/x-pkcs12"},
        { "pgm",    "image/x-portable-graymap"},
        { "pko",    "application/ynd.ms-pkipko"},
        { "pma",    "application/x-perfmon"},
        { "pmc",    "application/x-perfmon"},
        { "pml",    "application/x-perfmon"},
        { "pmr",    "application/x-perfmon"},
        { "pmw",    "application/x-perfmon"},
        { "png",    "image/png"},
        { "pnm",    "image/x-portable-anymap"},
        { "pot,",   "application/vnd.ms-powerpoint"},
        { "ppm",    "image/x-portable-pixmap"},
        { "pps",    "application/vnd.ms-powerpoint"},
        { "ppt",    "application/vnd.ms-powerpoint"},
        { "prf",    "application/pics-rules"},
        { "ps",     "application/postscript"},
        { "pub",    "application/x-mspublisher"},
        { "qt",     "video/quicktime"},
        { "ra",     "audio/x-pn-realaudio"},
        { "ram",    "audio/x-pn-realaudio"},
        { "ras",    "image/x-cmu-raster"},
        { "rgb",    "image/x-rgb"},
        { "rmi",    "audio/mid"},
        { "roff",   "application/x-troff"},
        { "rtf",    "application/rtf"},
        { "rtx",    "text/richtext"},
        { "scd",    "application/x-msschedule"},
        { "sct",    "text/scriptlet"},
        { "setpay", "application/set-payment-initiation"},
        { "setreg", "application/set-registration-initiation"},
        { "sh",     "application/x-sh"},
        { "shar",   "application/x-shar"},
        { "sit",    "application/x-stuffit"},
        { "snd",    "audio/basic"},
        { "spc",    "application/x-pkcs7-certificates"},
        { "spl",    "application/futuresplash"},
        { "src",    "application/x-wais-source"},
        { "sst",    "application/vnd.ms-pkicertstore"},
        { "stl",    "application/vnd.ms-pkistl"},
        { "stm",    "text/html"},
        { "svg",    "image/svg+xml"},
        { "sv4cpio","application/x-sv4cpio"},
        { "sv4crc", "application/x-sv4crc"},
        { "swf",    "application/x-shockwave-flash"},
        { "t",      "application/x-troff"},
        { "tar",    "application/x-tar"},
        { "tcl",    "application/x-tcl"},
        { "tex",    "application/x-tex"},
        { "texi",   "application/x-texinfo"},
        { "texinfo","application/x-texinfo"},
        { "tgz",    "application/x-compressed"},
        { "tif",    "image/tiff"},
        { "tiff",   "image/tiff"},
        { "tr",     "application/x-troff"},
        { "trm",    "application/x-msterminal"},
        { "tsv",    "text/tab-separated-values"},
        { "txt",    "text/plain"},
        { "uls",    "text/iuls"},
        { "ustar",  "application/x-ustar"},
        { "vcf",    "text/x-vcard"},
        { "vrml",   "x-world/x-vrml"},
        { "wav",    "audio/x-wav"},
        { "wcm",    "application/vnd.ms-works"},
        { "wdb",    "application/vnd.ms-works"},
        { "wks",    "application/vnd.ms-works"},
        { "wmf",    "application/x-msmetafile"},
        { "wps",    "application/vnd.ms-works"},
        { "wri",    "application/x-mswrite"},
        { "wrl",    "x-world/x-vrml"},
        { "wrz",    "x-world/x-vrml"},
        { "xaf",    "x-world/x-vrml"},
        { "xbm",    "image/x-xbitmap"},
        { "xla",    "application/vnd.ms-excel"},
        { "xlc",    "application/vnd.ms-excel"},
        { "xlm",    "application/vnd.ms-excel"},
        { "xls",    "application/vnd.ms-excel"},
        { "xlt",    "application/vnd.ms-excel"},
        { "xlw",    "application/vnd.ms-excel"},
        { "xof",    "x-world/x-vrml"},
        { "xpm",    "image/x-xpixmap"},
        { "xwd",    "image/x-xwindowdump"},
        { "z",      "application/x-compress"},
        { "zip",    "application/zip"}
    };

    public static final int[] COLOR_CHIP_RES_IDS = new int[]
                                                         {
                                                             R.drawable.appointment_indicator_leftside_1,
                                                             R.drawable.appointment_indicator_leftside_2,
                                                             R.drawable.appointment_indicator_leftside_3,
                                                             R.drawable.appointment_indicator_leftside_4,
                                                             R.drawable.appointment_indicator_leftside_5,
                                                             R.drawable.appointment_indicator_leftside_6,
                                                             R.drawable.appointment_indicator_leftside_7,
                                                             R.drawable.appointment_indicator_leftside_8,
                                                             R.drawable.appointment_indicator_leftside_9,
                                                             R.drawable.appointment_indicator_leftside_10,
                                                             R.drawable.appointment_indicator_leftside_11,
                                                             R.drawable.appointment_indicator_leftside_12,
                                                             R.drawable.appointment_indicator_leftside_13,
                                                             R.drawable.appointment_indicator_leftside_14,
                                                             R.drawable.appointment_indicator_leftside_15,
                                                             R.drawable.appointment_indicator_leftside_16,
                                                             R.drawable.appointment_indicator_leftside_17,
                                                             R.drawable.appointment_indicator_leftside_18,
                                                             R.drawable.appointment_indicator_leftside_19,
                                                             R.drawable.appointment_indicator_leftside_20,
                                                             R.drawable.appointment_indicator_leftside_21,
                                                         };

    
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
            MailService.actionReschedule(context, wakeLockId);
        }
        Class[] classes = { MessageCompose.class, BootReceiver.class, MailService.class };

        for (Class clazz : classes)
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
            MailService.actionReschedule(context, wakeLockId);
        }

    }

    public static void save(SharedPreferences.Editor editor)
    {
        editor.putBoolean("enableDebugLogging", K9.DEBUG);
        editor.putBoolean("enableSensitiveLogging", K9.DEBUG_SENSITIVE);
        editor.putString("backgroundOperations", K9.backgroundOps.toString());
        editor.putBoolean("animations", mAnimations);
        editor.putBoolean("messageListLefthandedWidgets",mMessageListLefthandedWidgets);
        editor.putInt("theme", theme);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        app = this;
        Preferences prefs = Preferences.getPreferences(this);
        SharedPreferences sprefs = prefs.getPreferences();
        DEBUG = sprefs.getBoolean("enableDebugLogging", false);
        DEBUG_SENSITIVE = sprefs.getBoolean("enableSensitiveLogging", false);
        mAnimations = sprefs.getBoolean("animations", true);
        mMessageListLefthandedWidgets = sprefs.getBoolean("messageListLefthandedWidgets",false);


        try
        {
            setBackgroundOps(BACKGROUND_OPS.valueOf(sprefs.getString("backgroundOperations", "WHEN_CHECKED")));
        }
        catch (Exception e)
        {
            setBackgroundOps(BACKGROUND_OPS.WHEN_CHECKED);
        }

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

    public static void setBackgroundOps(BACKGROUND_OPS backgroundOps)
    {
        K9.backgroundOps = backgroundOps;
    }

    public static void setBackgroundOps(String nbackgroundOps)
    {
        K9.backgroundOps = BACKGROUND_OPS.valueOf(nbackgroundOps);
    }

    public static boolean isAnimations()
    {
        return mAnimations;
    }

    public static void setAnimations(boolean animations)
    {
        mAnimations = animations;
    }

    public static boolean messageListLefthandedWidgets()
    {
        return mMessageListLefthandedWidgets;
    }

    public static void setMessageListLefthandedWidgets(boolean lefty)
    {
        mMessageListLefthandedWidgets = lefty;
    }
}








