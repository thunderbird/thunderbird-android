
package com.fsck.k9.activity.setup;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Account.Searchable;
import com.fsck.k9.Account.ShowPictures;
import com.fsck.k9.K9;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ChooseIdentity;
import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.activity.ManageIdentities;
import com.fsck.k9.crypto.OpenPgpApiHelper;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.service.MailService;
import com.fsck.k9.ui.dialog.AutocryptPreferEncryptDialog;
import com.fsck.k9.ui.dialog.AutocryptPreferEncryptDialog.OnPreferEncryptChangedListener;
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity;
import org.openintents.openpgp.OpenPgpApiManager;
import org.openintents.openpgp.util.OpenPgpKeyPreference;
import org.openintents.openpgp.util.OpenPgpProviderUtil;
import timber.log.Timber;


public class AccountSettings extends K9PreferenceActivity {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_SHOW_OPENPGP = "show_openpgp";

    private static final int DIALOG_COLOR_PICKER_ACCOUNT = 1;
    private static final int DIALOG_COLOR_PICKER_LED = 2;
    private static final int DIALOG_AUTOCRYPT_PREFER_ENCRYPT = 3;

    private static final int ACTIVITY_MANAGE_IDENTITIES = 2;
    private static final int ACTIVITY_CRYPTO_TRANSFER_KEY = 3;

    private static final String PREFERENCE_SCREEN_MAIN = "main";
    private static final String PREFERENCE_SCREEN_COMPOSING = "composing";
    private static final String PREFERENCE_SCREEN_INCOMING = "incoming_prefs";
    private static final String PREFERENCE_SCREEN_PUSH_ADVANCED = "push_advanced";
    private static final String PREFERENCE_SCREEN_SEARCH = "search";

    private static final String PREFERENCE_DESCRIPTION = "account_description";
    private static final String PREFERENCE_MARK_MESSAGE_AS_READ_ON_VIEW = "mark_message_as_read_on_view";
    private static final String PREFERENCE_COMPOSITION = "composition";
    private static final String PREFERENCE_MANAGE_IDENTITIES = "manage_identities";
    private static final String PREFERENCE_FREQUENCY = "account_check_frequency";
    private static final String PREFERENCE_DISPLAY_COUNT = "account_display_count";
    private static final String PREFERENCE_DEFAULT = "account_default";
    private static final String PREFERENCE_SHOW_PICTURES = "show_pictures_enum";
    private static final String PREFERENCE_NOTIFY = "account_notify";
    private static final String PREFERENCE_NOTIFY_NEW_MAIL_MODE = "folder_notify_new_mail_mode";
    private static final String PREFERENCE_NOTIFY_SELF = "account_notify_self";
    private static final String PREFERENCE_NOTIFY_CONTACTS_MAIL_ONLY = "account_notify_contacts_mail_only";
    private static final String PREFERENCE_NOTIFY_SYNC = "account_notify_sync";
    private static final String PREFERENCE_VIBRATE = "account_vibrate";
    private static final String PREFERENCE_VIBRATE_PATTERN = "account_vibrate_pattern";
    private static final String PREFERENCE_VIBRATE_TIMES = "account_vibrate_times";
    private static final String PREFERENCE_RINGTONE = "account_ringtone";
    private static final String PREFERENCE_NOTIFICATION_LED = "account_led";
    private static final String PREFERENCE_INCOMING = "incoming";
    private static final String PREFERENCE_OUTGOING = "outgoing";
    private static final String PREFERENCE_DISPLAY_MODE = "folder_display_mode";
    private static final String PREFERENCE_SYNC_MODE = "folder_sync_mode";
    private static final String PREFERENCE_PUSH_MODE = "folder_push_mode";
    private static final String PREFERENCE_PUSH_POLL_ON_CONNECT = "push_poll_on_connect";
    private static final String PREFERENCE_MAX_PUSH_FOLDERS = "max_push_folders";
    private static final String PREFERENCE_IDLE_REFRESH_PERIOD = "idle_refresh_period";
    private static final String PREFERENCE_TARGET_MODE = "folder_target_mode";
    private static final String PREFERENCE_DELETE_POLICY = "delete_policy";
    private static final String PREFERENCE_EXPUNGE_POLICY = "expunge_policy";
    private static final String PREFERENCE_AUTO_EXPAND_FOLDER = "account_setup_auto_expand_folder";
    private static final String PREFERENCE_SEARCHABLE_FOLDERS = "searchable_folders";
    private static final String PREFERENCE_CHIP_COLOR = "chip_color";
    private static final String PREFERENCE_LED_COLOR = "led_color";
    private static final String PREFERENCE_NOTIFICATION_OPENS_UNREAD = "notification_opens_unread";
    private static final String PREFERENCE_MESSAGE_AGE = "account_message_age";
    private static final String PREFERENCE_MESSAGE_SIZE = "account_autodownload_size";
    private static final String PREFERENCE_MESSAGE_FORMAT = "message_format";
    private static final String PREFERENCE_MESSAGE_READ_RECEIPT = "message_read_receipt";
    private static final String PREFERENCE_QUOTE_PREFIX = "account_quote_prefix";
    private static final String PREFERENCE_QUOTE_STYLE = "quote_style";
    private static final String PREFERENCE_DEFAULT_QUOTED_TEXT_SHOWN = "default_quoted_text_shown";
    private static final String PREFERENCE_REPLY_AFTER_QUOTE = "reply_after_quote";
    private static final String PREFERENCE_STRIP_SIGNATURE = "strip_signature";
    private static final String PREFERENCE_SYNC_REMOTE_DELETIONS = "account_sync_remote_deletetions";
    private static final String PREFERENCE_CRYPTO = "openpgp";
    private static final String PREFERENCE_CRYPTO_PROVIDER = "openpgp_provider";
    private static final String PREFERENCE_CRYPTO_KEY = "openpgp_key";
    private static final String PREFERENCE_CRYPTO_HIDE_SIGN_ONLY = "openpgp_hide_sign_only";
    private static final String PREFERENCE_AUTOCRYPT_PREFER_ENCRYPT = "autocrypt_prefer_encrypt";
    private static final String PREFERENCE_CRYPTO_ENCRYPT_SUBJECT = "openpgp_encrypt_subject";
    private static final String PREFERENCE_AUTOCRYPT_TRANSFER = "autocrypt_transfer";
    private static final String PREFERENCE_CLOUD_SEARCH_ENABLED = "remote_search_enabled";
    private static final String PREFERENCE_REMOTE_SEARCH_NUM_RESULTS = "account_remote_search_num_results";
    private static final String PREFERENCE_REMOTE_SEARCH_FULL_TEXT = "account_remote_search_full_text";

    private static final String PREFERENCE_LOCAL_STORAGE_PROVIDER = "local_storage_provider";
    private static final String PREFERENCE_CATEGORY_FOLDERS = "folders";
    private static final String PREFERENCE_ARCHIVE_FOLDER = "archive_folder";
    private static final String PREFERENCE_DRAFTS_FOLDER = "drafts_folder";
    private static final String PREFERENCE_SENT_FOLDER = "sent_folder";
    private static final String PREFERENCE_SPAM_FOLDER = "spam_folder";
    private static final String PREFERENCE_TRASH_FOLDER = "trash_folder";
    private static final String PREFERENCE_ALWAYS_SHOW_CC_BCC = "always_show_cc_bcc";


    private Account account;
    private boolean isMoveCapable = false;
    private boolean isPushCapable = false;
    private boolean isExpungeCapable = false;
    private boolean isSeenFlagSupported = false;

    private PreferenceScreen mainScreen;
    private PreferenceScreen composingScreen;

    private EditTextPreference accountDescription;
    private CheckBoxPreference markMessageAsReadOnView;
    private ListPreference checkFrequency;
    private ListPreference displayCount;
    private ListPreference messageAge;
    private ListPreference messageSize;
    private CheckBoxPreference accountDefault;
    private CheckBoxPreference accountNotify;
    private ListPreference accountNotifyNewMailMode;
    private CheckBoxPreference accountNotifySelf;
    private CheckBoxPreference accountNotifyContactsMailOnly;
    private ListPreference accountShowPictures;
    private CheckBoxPreference accountNotifySync;
    private CheckBoxPreference accountVibrateEnabled;
    private CheckBoxPreference accountLedEnabled;
    private ListPreference accountVibratePattern;
    private ListPreference accountVibrateTimes;
    private RingtonePreference accountRingtone;
    private ListPreference displayMode;
    private ListPreference syncMode;
    private ListPreference pushMode;
    private ListPreference targetMode;
    private ListPreference deletePolicy;
    private ListPreference expungePolicy;
    private ListPreference searchableFolders;
    private ListPreference autoExpandFolder;
    private Preference chipColor;
    private Preference ledColor;
    private boolean incomingChanged = false;
    private CheckBoxPreference notificationOpensUnread;
    private ListPreference messageFormat;
    private CheckBoxPreference messageReadReceipt;
    private ListPreference quoteStyle;
    private EditTextPreference accountQuotePrefix;
    private CheckBoxPreference accountDefaultQuotedTextShown;
    private CheckBoxPreference replyAfterQuote;
    private CheckBoxPreference stripSignature;
    private CheckBoxPreference syncRemoteDeletions;
    private CheckBoxPreference pushPollOnConnect;
    private ListPreference idleRefreshPeriod;
    private ListPreference mMaxPushFolders;
    private OpenPgpKeyPreference pgpCryptoKey;
    private Preference autocryptPreferEncryptMutual;
    private SwitchPreference pgpHideSignOnly;
    private SwitchPreference pgpEncryptSubject;

    private PreferenceScreen searchScreen;
    private CheckBoxPreference cloudSearchEnabled;
    private ListPreference remoteSearchNumResults;

    /*
     * Temporarily removed because search results aren't displayed to the user.
     * So this feature is useless.
     */
    //private CheckBoxPreference mRemoteSearchFullText;

    private ListPreference localStorageProvider;
    private ListPreference archiveFolder;
    private ListPreference draftsFolder;
    private ListPreference sentFolder;
    private ListPreference spamFolder;
    private ListPreference trashFolder;
    private CheckBoxPreference alwaysShowCcBcc;
    private SwitchPreference pgpEnable;
    private OpenPgpApiManager openPgpApiManager;


    public static void actionSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSettings.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    public static void actionSettingsOpenPgp(Context context, Account account) {
        Intent i = new Intent(context, AccountSettings.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_SHOW_OPENPGP, true);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        openPgpApiManager = new OpenPgpApiManager(getApplicationContext(), getLifecycle());

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);
        boolean startInOpenPgp = getIntent().getBooleanExtra(EXTRA_SHOW_OPENPGP, false);

        try {
            RemoteStore store = account.getRemoteStore();
            isMoveCapable = store.isMoveCapable();
            isPushCapable = store.isPushCapable();
            isExpungeCapable = store.isExpungeCapable();
            isSeenFlagSupported = store.isSeenFlagSupported();
        } catch (Exception e) {
            Timber.e(e, "Could not get remote store");
        }

        addPreferencesFromResource(R.xml.account_settings_preferences);

        mainScreen = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_MAIN);

        accountDescription = (EditTextPreference) findPreference(PREFERENCE_DESCRIPTION);
        accountDescription.setSummary(account.getDescription());
        accountDescription.setText(account.getDescription());
        accountDescription.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                accountDescription.setSummary(summary);
                accountDescription.setText(summary);
                return false;
            }
        });

        markMessageAsReadOnView = (CheckBoxPreference) findPreference(PREFERENCE_MARK_MESSAGE_AS_READ_ON_VIEW);
        markMessageAsReadOnView.setChecked(account.isMarkMessageAsReadOnView());

        messageFormat = (ListPreference) findPreference(PREFERENCE_MESSAGE_FORMAT);
        messageFormat.setValue(account.getMessageFormat().name());
        messageFormat.setSummary(messageFormat.getEntry());
        messageFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = messageFormat.findIndexOfValue(summary);
                messageFormat.setSummary(messageFormat.getEntries()[index]);
                messageFormat.setValue(summary);
                return false;
            }
        });

        alwaysShowCcBcc = (CheckBoxPreference) findPreference(PREFERENCE_ALWAYS_SHOW_CC_BCC);
        alwaysShowCcBcc.setChecked(account.isAlwaysShowCcBcc());

        messageReadReceipt = (CheckBoxPreference) findPreference(PREFERENCE_MESSAGE_READ_RECEIPT);
        messageReadReceipt.setChecked(account.isMessageReadReceiptAlways());

        accountQuotePrefix = (EditTextPreference) findPreference(PREFERENCE_QUOTE_PREFIX);
        accountQuotePrefix.setSummary(account.getQuotePrefix());
        accountQuotePrefix.setText(account.getQuotePrefix());
        accountQuotePrefix.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = newValue.toString();
                accountQuotePrefix.setSummary(value);
                accountQuotePrefix.setText(value);
                return false;
            }
        });

        accountDefaultQuotedTextShown = (CheckBoxPreference) findPreference(PREFERENCE_DEFAULT_QUOTED_TEXT_SHOWN);
        accountDefaultQuotedTextShown.setChecked(account.isDefaultQuotedTextShown());

        replyAfterQuote = (CheckBoxPreference) findPreference(PREFERENCE_REPLY_AFTER_QUOTE);
        replyAfterQuote.setChecked(account.isReplyAfterQuote());

        stripSignature = (CheckBoxPreference) findPreference(PREFERENCE_STRIP_SIGNATURE);
        stripSignature.setChecked(account.isStripSignature());

        composingScreen = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_COMPOSING);

        Preference.OnPreferenceChangeListener quoteStyleListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final QuoteStyle style = QuoteStyle.valueOf(newValue.toString());
                int index = quoteStyle.findIndexOfValue(newValue.toString());
                quoteStyle.setSummary(quoteStyle.getEntries()[index]);
                if (style == QuoteStyle.PREFIX) {
                    composingScreen.addPreference(accountQuotePrefix);
                    composingScreen.addPreference(replyAfterQuote);
                } else if (style == QuoteStyle.HEADER) {
                    composingScreen.removePreference(accountQuotePrefix);
                    composingScreen.removePreference(replyAfterQuote);
                }
                return true;
            }
        };
        quoteStyle = (ListPreference) findPreference(PREFERENCE_QUOTE_STYLE);
        quoteStyle.setValue(account.getQuoteStyle().name());
        quoteStyle.setSummary(quoteStyle.getEntry());
        quoteStyle.setOnPreferenceChangeListener(quoteStyleListener);
        // Call the onPreferenceChange() handler on startup to update the Preference dialogue based
        // upon the existing quote style setting.
        quoteStyleListener.onPreferenceChange(quoteStyle, account.getQuoteStyle().name());

        checkFrequency = (ListPreference) findPreference(PREFERENCE_FREQUENCY);
        checkFrequency.setValue(String.valueOf(account.getAutomaticCheckIntervalMinutes()));
        checkFrequency.setSummary(checkFrequency.getEntry());
        checkFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = checkFrequency.findIndexOfValue(summary);
                checkFrequency.setSummary(checkFrequency.getEntries()[index]);
                checkFrequency.setValue(summary);
                return false;
            }
        });

        displayMode = (ListPreference) findPreference(PREFERENCE_DISPLAY_MODE);
        displayMode.setValue(account.getFolderDisplayMode().name());
        displayMode.setSummary(displayMode.getEntry());
        displayMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = displayMode.findIndexOfValue(summary);
                displayMode.setSummary(displayMode.getEntries()[index]);
                displayMode.setValue(summary);
                return false;
            }
        });

        syncMode = (ListPreference) findPreference(PREFERENCE_SYNC_MODE);
        syncMode.setValue(account.getFolderSyncMode().name());
        syncMode.setSummary(syncMode.getEntry());
        syncMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = syncMode.findIndexOfValue(summary);
                syncMode.setSummary(syncMode.getEntries()[index]);
                syncMode.setValue(summary);
                return false;
            }
        });


        targetMode = (ListPreference) findPreference(PREFERENCE_TARGET_MODE);
        targetMode.setValue(account.getFolderTargetMode().name());
        targetMode.setSummary(targetMode.getEntry());
        targetMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = targetMode.findIndexOfValue(summary);
                targetMode.setSummary(targetMode.getEntries()[index]);
                targetMode.setValue(summary);
                return false;
            }
        });

        deletePolicy = (ListPreference) findPreference(PREFERENCE_DELETE_POLICY);
        if (!isSeenFlagSupported) {
            removeListEntry(deletePolicy, DeletePolicy.MARK_AS_READ.preferenceString());
        }
        deletePolicy.setValue(account.getDeletePolicy().preferenceString());
        deletePolicy.setSummary(deletePolicy.getEntry());
        deletePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = deletePolicy.findIndexOfValue(summary);
                deletePolicy.setSummary(deletePolicy.getEntries()[index]);
                deletePolicy.setValue(summary);
                return false;
            }
        });


        expungePolicy = (ListPreference) findPreference(PREFERENCE_EXPUNGE_POLICY);
        if (isExpungeCapable) {
            expungePolicy.setValue(account.getExpungePolicy().name());
            expungePolicy.setSummary(expungePolicy.getEntry());
            expungePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = expungePolicy.findIndexOfValue(summary);
                    expungePolicy.setSummary(expungePolicy.getEntries()[index]);
                    expungePolicy.setValue(summary);
                    return false;
                }
            });
        } else {
            ((PreferenceScreen) findPreference(PREFERENCE_SCREEN_INCOMING)).removePreference(expungePolicy);
        }


        syncRemoteDeletions = (CheckBoxPreference) findPreference(PREFERENCE_SYNC_REMOTE_DELETIONS);
        syncRemoteDeletions.setChecked(account.syncRemoteDeletions());

        searchableFolders = (ListPreference) findPreference(PREFERENCE_SEARCHABLE_FOLDERS);
        searchableFolders.setValue(account.getSearchableFolders().name());
        searchableFolders.setSummary(searchableFolders.getEntry());
        searchableFolders.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = searchableFolders.findIndexOfValue(summary);
                searchableFolders.setSummary(searchableFolders.getEntries()[index]);
                searchableFolders.setValue(summary);
                return false;
            }
        });

        displayCount = (ListPreference) findPreference(PREFERENCE_DISPLAY_COUNT);
        displayCount.setValue(String.valueOf(account.getDisplayCount()));
        displayCount.setSummary(displayCount.getEntry());
        displayCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = displayCount.findIndexOfValue(summary);
                displayCount.setSummary(displayCount.getEntries()[index]);
                displayCount.setValue(summary);
                return false;
            }
        });



        messageAge = (ListPreference) findPreference(PREFERENCE_MESSAGE_AGE);

        if (!account.isSearchByDateCapable()) {
            ((PreferenceScreen) findPreference(PREFERENCE_SCREEN_INCOMING)).removePreference(messageAge);
        } else {
            messageAge.setValue(String.valueOf(account.getMaximumPolledMessageAge()));
            messageAge.setSummary(messageAge.getEntry());
            messageAge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = messageAge.findIndexOfValue(summary);
                    messageAge.setSummary(messageAge.getEntries()[index]);
                    messageAge.setValue(summary);
                    return false;
                }
            });

        }

        messageSize = (ListPreference) findPreference(PREFERENCE_MESSAGE_SIZE);
        messageSize.setValue(String.valueOf(account.getMaximumAutoDownloadMessageSize()));
        messageSize.setSummary(messageSize.getEntry());
        messageSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = messageSize.findIndexOfValue(summary);
                messageSize.setSummary(messageSize.getEntries()[index]);
                messageSize.setValue(summary);
                return false;
            }
        });

        accountDefault = (CheckBoxPreference) findPreference(PREFERENCE_DEFAULT);
        accountDefault.setChecked(
            account.equals(Preferences.getPreferences(this).getDefaultAccount()));

        accountShowPictures = (ListPreference) findPreference(PREFERENCE_SHOW_PICTURES);
        accountShowPictures.setValue("" + account.getShowPictures());
        accountShowPictures.setSummary(accountShowPictures.getEntry());
        accountShowPictures.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = accountShowPictures.findIndexOfValue(summary);
                accountShowPictures.setSummary(accountShowPictures.getEntries()[index]);
                accountShowPictures.setValue(summary);
                return false;
            }
        });


        localStorageProvider = (ListPreference) findPreference(PREFERENCE_LOCAL_STORAGE_PROVIDER);
        {
            final Map<String, String> providers;
            providers = StorageManager.getInstance(this).getAvailableProviders();
            int i = 0;
            final String[] providerLabels = new String[providers.size()];
            final String[] providerIds = new String[providers.size()];
            for (final Map.Entry<String, String> entry : providers.entrySet()) {
                providerIds[i] = entry.getKey();
                providerLabels[i] = entry.getValue();
                i++;
            }
            localStorageProvider.setEntryValues(providerIds);
            localStorageProvider.setEntries(providerLabels);
            localStorageProvider.setValue(account.getLocalStorageProviderId());
            localStorageProvider.setSummary(providers.get(account.getLocalStorageProviderId()));

            localStorageProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    localStorageProvider.setSummary(providers.get(newValue));
                    return true;
                }
            });
        }

        // IMAP-specific preferences

        searchScreen = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_SEARCH);

        cloudSearchEnabled = (CheckBoxPreference) findPreference(PREFERENCE_CLOUD_SEARCH_ENABLED);
        remoteSearchNumResults = (ListPreference) findPreference(PREFERENCE_REMOTE_SEARCH_NUM_RESULTS);
        remoteSearchNumResults.setOnPreferenceChangeListener(
            new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newVal) {
                    updateRemoteSearchLimit((String)newVal);
                    return true;
                }
            }
        );
        //mRemoteSearchFullText = (CheckBoxPreference) findPreference(PREFERENCE_REMOTE_SEARCH_FULL_TEXT);

        pushPollOnConnect = (CheckBoxPreference) findPreference(PREFERENCE_PUSH_POLL_ON_CONNECT);
        idleRefreshPeriod = (ListPreference) findPreference(PREFERENCE_IDLE_REFRESH_PERIOD);
        mMaxPushFolders = (ListPreference) findPreference(PREFERENCE_MAX_PUSH_FOLDERS);
        if (isPushCapable) {
            pushPollOnConnect.setChecked(account.isPushPollOnConnect());

            cloudSearchEnabled.setChecked(account.allowRemoteSearch());
            String searchNumResults = Integer.toString(account.getRemoteSearchNumResults());
            remoteSearchNumResults.setValue(searchNumResults);
            updateRemoteSearchLimit(searchNumResults);
            //mRemoteSearchFullText.setChecked(account.isRemoteSearchFullText());

            idleRefreshPeriod.setValue(String.valueOf(account.getIdleRefreshMinutes()));
            idleRefreshPeriod.setSummary(idleRefreshPeriod.getEntry());
            idleRefreshPeriod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = idleRefreshPeriod.findIndexOfValue(summary);
                    idleRefreshPeriod.setSummary(idleRefreshPeriod.getEntries()[index]);
                    idleRefreshPeriod.setValue(summary);
                    return false;
                }
            });

            mMaxPushFolders.setValue(String.valueOf(account.getMaxPushFolders()));
            mMaxPushFolders.setSummary(mMaxPushFolders.getEntry());
            mMaxPushFolders.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mMaxPushFolders.findIndexOfValue(summary);
                    mMaxPushFolders.setSummary(mMaxPushFolders.getEntries()[index]);
                    mMaxPushFolders.setValue(summary);
                    return false;
                }
            });
            pushMode = (ListPreference) findPreference(PREFERENCE_PUSH_MODE);
            pushMode.setValue(account.getFolderPushMode().name());
            pushMode.setSummary(pushMode.getEntry());
            pushMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = pushMode.findIndexOfValue(summary);
                    pushMode.setSummary(pushMode.getEntries()[index]);
                    pushMode.setValue(summary);
                    return false;
                }
            });
        } else {
            PreferenceScreen incomingPrefs = (PreferenceScreen) findPreference(PREFERENCE_SCREEN_INCOMING);
            incomingPrefs.removePreference(findPreference(PREFERENCE_SCREEN_PUSH_ADVANCED));
            incomingPrefs.removePreference(findPreference(PREFERENCE_PUSH_MODE));
            mainScreen.removePreference(searchScreen);
        }

        accountNotify = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY);
        accountNotify.setChecked(account.isNotifyNewMail());

        accountNotifyNewMailMode = (ListPreference) findPreference(PREFERENCE_NOTIFY_NEW_MAIL_MODE);
        accountNotifyNewMailMode.setValue(account.getFolderNotifyNewMailMode().name());
        accountNotifyNewMailMode.setSummary(accountNotifyNewMailMode.getEntry());
        accountNotifyNewMailMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = accountNotifyNewMailMode.findIndexOfValue(summary);
                accountNotifyNewMailMode.setSummary(accountNotifyNewMailMode.getEntries()[index]);
                accountNotifyNewMailMode.setValue(summary);
                return false;
            }
        });

        accountNotifySelf = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY_SELF);
        accountNotifySelf.setChecked(account.isNotifySelfNewMail());

        accountNotifyContactsMailOnly = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY_CONTACTS_MAIL_ONLY);
        accountNotifyContactsMailOnly.setChecked(account.isNotifyContactsMailOnly());

        accountNotifySync = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY_SYNC);
        accountNotifySync.setChecked(account.isShowOngoing());

        accountRingtone = (RingtonePreference) findPreference(PREFERENCE_RINGTONE);

        // XXX: The following two lines act as a workaround for the RingtonePreference
        //      which does not let us set/get the value programmatically
        SharedPreferences prefs = accountRingtone.getPreferenceManager().getSharedPreferences();
        String currentRingtone = (!account.getNotificationSetting().isRingEnabled() ? null : account.getNotificationSetting().getRingtone());
        prefs.edit().putString(PREFERENCE_RINGTONE, currentRingtone).commit();

        accountVibrateEnabled = (CheckBoxPreference) findPreference(PREFERENCE_VIBRATE);
        accountVibrateEnabled.setChecked(account.getNotificationSetting().isVibrateEnabled());

        accountVibratePattern = (ListPreference) findPreference(PREFERENCE_VIBRATE_PATTERN);
        accountVibratePattern.setValue(String.valueOf(account.getNotificationSetting().getVibratePattern()));
        accountVibratePattern.setSummary(accountVibratePattern.getEntry());
        accountVibratePattern.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = accountVibratePattern.findIndexOfValue(summary);
                accountVibratePattern.setSummary(accountVibratePattern.getEntries()[index]);
                accountVibratePattern.setValue(summary);
                doVibrateTest(preference);
                return false;
            }
        });

        accountVibrateTimes = (ListPreference) findPreference(PREFERENCE_VIBRATE_TIMES);
        accountVibrateTimes.setValue(String.valueOf(account.getNotificationSetting().getVibrateTimes()));
        accountVibrateTimes.setSummary(String.valueOf(account.getNotificationSetting().getVibrateTimes()));
        accountVibrateTimes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = newValue.toString();
                accountVibrateTimes.setSummary(value);
                accountVibrateTimes.setValue(value);
                doVibrateTest(preference);
                return false;
            }
        });

        accountLedEnabled = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFICATION_LED);
        accountLedEnabled.setChecked(account.getNotificationSetting().isLedEnabled());

        notificationOpensUnread = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFICATION_OPENS_UNREAD);
        notificationOpensUnread.setChecked(account.goToUnreadMessageSearch());

        new PopulateFolderPrefsTask().execute();

        chipColor = findPreference(PREFERENCE_CHIP_COLOR);
        chipColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onChooseChipColor();
                return false;
            }
        });

        ledColor = findPreference(PREFERENCE_LED_COLOR);
        ledColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onChooseLedColor();
                return false;
            }
        });

        findPreference(PREFERENCE_COMPOSITION).setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onCompositionSettings();
                return true;
            }
        });

        findPreference(PREFERENCE_MANAGE_IDENTITIES).setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onManageIdentities();
                return true;
            }
        });

        findPreference(PREFERENCE_INCOMING).setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                incomingChanged = true;
                onIncomingSettings();
                return true;
            }
        });

        findPreference(PREFERENCE_OUTGOING).setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onOutgoingSettings();
                return true;
            }
        });

        setupCryptoSettings();

        if (savedInstanceState == null && startInOpenPgp) {
            PreferenceScreen preference = (PreferenceScreen) findPreference(PREFERENCE_CRYPTO);
            goToPreferenceScreen(preference);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // we might return here from the play store, so it's important we refresh on resume
        setupCryptoSettings();
    }

    private void setupCryptoSettings() {
        pgpEnable = (SwitchPreference) findPreference(PREFERENCE_CRYPTO_PROVIDER);
        pgpCryptoKey = (OpenPgpKeyPreference) findPreference(PREFERENCE_CRYPTO_KEY);
        autocryptPreferEncryptMutual = findPreference(PREFERENCE_AUTOCRYPT_PREFER_ENCRYPT);
        pgpHideSignOnly = (SwitchPreference) findPreference(PREFERENCE_CRYPTO_HIDE_SIGN_ONLY);
        pgpEncryptSubject = (SwitchPreference) findPreference(PREFERENCE_CRYPTO_ENCRYPT_SUBJECT);

        String pgpProvider = account.getOpenPgpProvider();
        String pgpProviderName = null;
        boolean isPgpConfigured = account.isOpenPgpProviderConfigured();
        boolean isKeyConfigured = account.hasOpenPgpKey();

        if (isPgpConfigured) {
            pgpProviderName = OpenPgpProviderUtil.getOpenPgpProviderName(getPackageManager(), pgpProvider);

            if (pgpProviderName == null) {
                Toast.makeText(this, R.string.account_settings_openpgp_missing, Toast.LENGTH_LONG).show();
                account.setOpenPgpProvider(null);
                pgpProvider = null;
                isPgpConfigured = false;
            }
        }

        if (!isPgpConfigured) {
            pgpEnable.setChecked(false);
            pgpEnable.setSummary(R.string.account_settings_crypto_summary_off);
            pgpEnable.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    pgpEnable.setOnPreferenceClickListener(null);
                    List<String> openPgpProviderPackages =
                            OpenPgpProviderUtil.getOpenPgpProviderPackages(getApplicationContext());
                    if (openPgpProviderPackages.size() == 1) {
                        account.setOpenPgpProvider(openPgpProviderPackages.get(0));
                        setupCryptoSettings();
                    } else {
                        OpenPgpAppSelectDialog.startOpenPgpChooserActivity(AccountSettings.this, account);
                    }
                    return false;
                }
            });
        } else {
            pgpEnable.setChecked(true);
            pgpEnable.setSummary(getString(R.string.account_settings_crypto_summary_on, pgpProviderName));
            pgpEnable.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    pgpEnable.setOnPreferenceClickListener(null);
                    account.setOpenPgpProvider(null);
                    account.setOpenPgpKey(Account.NO_OPENPGP_KEY);
                    setupCryptoSettings();
                    return true;
                }
            });
        }

        Preference autocryptTransfer = findPreference(PREFERENCE_AUTOCRYPT_TRANSFER);
        autocryptTransfer.setEnabled(isPgpConfigured && isKeyConfigured);
        autocryptTransfer.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onClickAutocryptTransfer();
                return true;
            }
        });

        pgpCryptoKey.setValue(account.getOpenPgpKey());
        pgpCryptoKey.setOpenPgpProvider(openPgpApiManager, pgpProvider);
        pgpCryptoKey.setDefaultUserId(OpenPgpApiHelper.buildUserId(account.getIdentity(0)));
        pgpCryptoKey.setShowAutocryptHint(true);
        pgpCryptoKey.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                long value = (Long) newValue;
                account.setOpenPgpKey(value);
                setupCryptoSettings();
                return false;
            }
        });

        autocryptPreferEncryptMutual.setEnabled(isPgpConfigured && isKeyConfigured);
        autocryptPreferEncryptMutual.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(DIALOG_AUTOCRYPT_PREFER_ENCRYPT);
                return false;
            }
        });

        pgpHideSignOnly.setEnabled(isPgpConfigured && isKeyConfigured);
        pgpHideSignOnly.setChecked(account.getOpenPgpHideSignOnly());

        pgpEncryptSubject.setEnabled(isPgpConfigured && isKeyConfigured);
        pgpEncryptSubject.setChecked(account.getOpenPgpEncryptSubject());
    }

    private void onClickAutocryptTransfer() {
        Intent intent = AutocryptKeyTransferActivity.Companion.createIntent(this, account.getUuid());
        startActivity(intent);
    }

    private void removeListEntry(ListPreference listPreference, String remove) {
        CharSequence[] entryValues = listPreference.getEntryValues();
        CharSequence[] entries = listPreference.getEntries();

        CharSequence[] newEntryValues = new String[entryValues.length - 1];
        CharSequence[] newEntries = new String[entryValues.length - 1];

        for (int i = 0, out = 0; i < entryValues.length; i++) {
            CharSequence value = entryValues[i];
            if (!value.equals(remove)) {
                newEntryValues[out] = value;
                newEntries[out] = entries[i];
                out++;
            }
        }

        listPreference.setEntryValues(newEntryValues);
        listPreference.setEntries(newEntries);
    }

    private void saveSettings() {
        if (accountDefault.isChecked()) {
            Preferences.getPreferences(this).setDefaultAccount(account);
        }

        account.setDescription(accountDescription.getText());
        account.setMarkMessageAsReadOnView(markMessageAsReadOnView.isChecked());
        account.setNotifyNewMail(accountNotify.isChecked());
        account.setFolderNotifyNewMailMode(FolderMode.valueOf(accountNotifyNewMailMode.getValue()));
        account.setNotifySelfNewMail(accountNotifySelf.isChecked());
        account.setNotifyContactsMailOnly(accountNotifyContactsMailOnly.isChecked());
        account.setShowOngoing(accountNotifySync.isChecked());
        account.setDisplayCount(Integer.parseInt(displayCount.getValue()));
        account.setMaximumAutoDownloadMessageSize(Integer.parseInt(messageSize.getValue()));
        if (account.isSearchByDateCapable()) {
            account.setMaximumPolledMessageAge(Integer.parseInt(messageAge.getValue()));
        }
        account.getNotificationSetting().setVibrate(accountVibrateEnabled.isChecked());
        account.getNotificationSetting().setVibratePattern(Integer.parseInt(accountVibratePattern.getValue()));
        account.getNotificationSetting().setVibrateTimes(Integer.parseInt(accountVibrateTimes.getValue()));
        account.getNotificationSetting().setLed(accountLedEnabled.isChecked());
        account.setGoToUnreadMessageSearch(notificationOpensUnread.isChecked());
        account.setFolderTargetMode(FolderMode.valueOf(targetMode.getValue()));
        account.setDeletePolicy(DeletePolicy.fromInt(Integer.parseInt(deletePolicy.getValue())));
        if (isExpungeCapable) {
            account.setExpungePolicy(Expunge.valueOf(expungePolicy.getValue()));
        }
        account.setSyncRemoteDeletions(syncRemoteDeletions.isChecked());
        account.setSearchableFolders(Searchable.valueOf(searchableFolders.getValue()));
        account.setMessageFormat(MessageFormat.valueOf(messageFormat.getValue()));
        account.setAlwaysShowCcBcc(alwaysShowCcBcc.isChecked());
        account.setMessageReadReceipt(messageReadReceipt.isChecked());
        account.setQuoteStyle(QuoteStyle.valueOf(quoteStyle.getValue()));
        account.setQuotePrefix(accountQuotePrefix.getText());
        account.setDefaultQuotedTextShown(accountDefaultQuotedTextShown.isChecked());
        account.setReplyAfterQuote(replyAfterQuote.isChecked());
        account.setStripSignature(stripSignature.isChecked());
        account.setLocalStorageProviderId(localStorageProvider.getValue());
        if (pgpCryptoKey != null) {
            account.setOpenPgpKey(pgpCryptoKey.getValue());
        }
        account.setOpenPgpHideSignOnly(pgpHideSignOnly.isChecked());
        account.setOpenPgpEncryptSubject(pgpEncryptSubject.isChecked());

        account.setAutoExpandFolder(autoExpandFolder.getValue());

        if (isMoveCapable) {
            account.setArchiveFolder(archiveFolder.getValue());
            account.setDraftsFolder(draftsFolder.getValue());
            account.setSentFolder(sentFolder.getValue());
            account.setSpamFolder(spamFolder.getValue());
            account.setTrashFolder(trashFolder.getValue());
        }

        //IMAP stuff
        if (isPushCapable) {
            account.setPushPollOnConnect(pushPollOnConnect.isChecked());
            account.setIdleRefreshMinutes(Integer.parseInt(idleRefreshPeriod.getValue()));
            account.setMaxPushFolders(Integer.parseInt(mMaxPushFolders.getValue()));
            account.setAllowRemoteSearch(cloudSearchEnabled.isChecked());
            account.setRemoteSearchNumResults(Integer.parseInt(remoteSearchNumResults.getValue()));
            //account.setRemoteSearchFullText(mRemoteSearchFullText.isChecked());
        }

        boolean needsRefresh = account.setAutomaticCheckIntervalMinutes(Integer.parseInt(checkFrequency.getValue()));
        needsRefresh |= account.setFolderSyncMode(FolderMode.valueOf(syncMode.getValue()));

        boolean displayModeChanged = account.setFolderDisplayMode(FolderMode.valueOf(displayMode.getValue()));

        SharedPreferences prefs = accountRingtone.getPreferenceManager().getSharedPreferences();
        String newRingtone = prefs.getString(PREFERENCE_RINGTONE, null);
        if (newRingtone != null) {
            account.getNotificationSetting().setRingEnabled(true);
            account.getNotificationSetting().setRingtone(newRingtone);
        } else {
            if (account.getNotificationSetting().isRingEnabled()) {
                account.getNotificationSetting().setRingtone(null);
            }
        }

        account.setShowPictures(ShowPictures.valueOf(accountShowPictures.getValue()));

        //IMAP specific stuff
        if (isPushCapable) {
            boolean needsPushRestart = account.setFolderPushMode(FolderMode.valueOf(pushMode.getValue()));
            if (account.getFolderPushMode() != FolderMode.NONE) {
                needsPushRestart |= displayModeChanged;
                needsPushRestart |= incomingChanged;
            }

            if (needsRefresh && needsPushRestart) {
                MailService.actionReset(this, null);
            } else if (needsRefresh) {
                MailService.actionReschedulePoll(this, null);
            } else if (needsPushRestart) {
                MailService.actionRestartPushers(this, null);
            }
        }
        // TODO: refresh folder list here
        account.save(Preferences.getPreferences(this));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (pgpCryptoKey != null && pgpCryptoKey.handleOnActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        saveSettings();
        super.onPause();
    }

    private void onCompositionSettings() {
        AccountSetupComposition.actionEditCompositionSettings(this, account);
    }

    private void onManageIdentities() {
        Intent intent = new Intent(this, ManageIdentities.class);
        intent.putExtra(ChooseIdentity.EXTRA_ACCOUNT, account.getUuid());
        startActivityForResult(intent, ACTIVITY_MANAGE_IDENTITIES);
    }

    private void onIncomingSettings() {
        AccountSetupIncoming.actionEditIncomingSettings(this, account);
    }

    private void onOutgoingSettings() {
        AccountSetupOutgoing.actionEditOutgoingSettings(this, account);
    }

    public void onChooseChipColor() {
        showDialog(DIALOG_COLOR_PICKER_ACCOUNT);
    }


    public void onChooseLedColor() {
        showDialog(DIALOG_COLOR_PICKER_LED);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
            case DIALOG_COLOR_PICKER_ACCOUNT: {
                dialog = new ColorPickerDialog(this,
                        new ColorPickerDialog.OnColorChangedListener() {
                            public void colorChanged(int color) {
                                account.setChipColor(color);
                            }
                        },
                        account.getChipColor());

                break;
            }
            case DIALOG_COLOR_PICKER_LED: {
                dialog = new ColorPickerDialog(this,
                        new ColorPickerDialog.OnColorChangedListener() {
                            public void colorChanged(int color) {
                                account.getNotificationSetting().setLedColor(color);
                            }
                        },
                        account.getNotificationSetting().getLedColor());

                break;
            }
            case DIALOG_AUTOCRYPT_PREFER_ENCRYPT: {
                dialog = new AutocryptPreferEncryptDialog(this, account.getAutocryptPreferEncryptMutual(),
                        new OnPreferEncryptChangedListener() {
                            @Override
                            public void onPreferEncryptChanged(boolean enabled) {
                                account.setAutocryptPreferEncryptMutual(enabled);
                            }
                        });
                break;
            }
        }

        return dialog;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_COLOR_PICKER_ACCOUNT: {
                ColorPickerDialog colorPicker = (ColorPickerDialog) dialog;
                colorPicker.setColor(account.getChipColor());
                break;
            }
            case DIALOG_COLOR_PICKER_LED: {
                ColorPickerDialog colorPicker = (ColorPickerDialog) dialog;
                colorPicker.setColor(account.getNotificationSetting().getLedColor());
                break;
            }
        }
    }

    private String getFolderDisplayName(String folderServerId, String folderName) {
        if (account.getInboxFolder().equals(folderServerId)) {
            return getString(R.string.special_mailbox_name_inbox);
        } else {
            return folderName;
        }
    }

    private void doVibrateTest(Preference preference) {
        // Do the vibration to show the user what it's like.
        Vibrator vibrate = (Vibrator)preference.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrate.vibrate(NotificationSetting.getVibration(
                            Integer.parseInt(accountVibratePattern.getValue()),
                            Integer.parseInt(accountVibrateTimes.getValue())), -1);
    }

    /**
     * Remote search result limit summary contains the current limit.  On load or change, update this value.
     * @param maxResults Search limit to update the summary with.
     */
    private void updateRemoteSearchLimit(String maxResults) {
        if (maxResults != null) {
            if (maxResults.equals("0")) {
                maxResults = getString(R.string.account_settings_remote_search_num_results_entries_all);
            }

            remoteSearchNumResults
                    .setSummary(String.format(getString(R.string.account_settings_remote_search_num_summary), maxResults));
        }
    }

    private void goToPreferenceScreen(PreferenceScreen preference) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        ListAdapter listAdapter = preferenceScreen.getRootAdapter();
        for (int itemNumber = 0; itemNumber < listAdapter.getCount(); itemNumber++) {
            if (listAdapter.getItem(itemNumber).equals(preference)) {
                preferenceScreen.onItemClick(null, null, itemNumber, 0);
                break;
            }
        }
    }

    private class PopulateFolderPrefsTask extends AsyncTask<Void, Void, Void> {
        List <? extends Folder > folders = new LinkedList<>();
        String[] allFolderValues;
        String[] allFolderLabels;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                folders = account.getLocalStore().getPersonalNamespaces(false);
            } catch (Exception e) {
                /// this can't be checked in
            }

            // TODO: In the future the call above should be changed to only return remote folders.
            // For now we just remove the Outbox folder if present.
            Iterator <? extends Folder > iter = folders.iterator();
            while (iter.hasNext()) {
                Folder folder = iter.next();
                if (account.getOutboxFolder().equals(folder.getServerId())) {
                    iter.remove();
                }
            }

            allFolderValues = new String[folders.size() + 1];
            allFolderLabels = new String[folders.size() + 1];

            allFolderValues[0] = K9.FOLDER_NONE;
            allFolderLabels[0] = K9.FOLDER_NONE;

            int i = 1;
            for (Folder folder : folders) {
                allFolderLabels[i] = getFolderDisplayName(folder.getServerId(), folder.getName());
                allFolderValues[i] = folder.getServerId();
                i++;
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            autoExpandFolder = (ListPreference) findPreference(PREFERENCE_AUTO_EXPAND_FOLDER);
            autoExpandFolder.setEnabled(false);
            archiveFolder = (ListPreference) findPreference(PREFERENCE_ARCHIVE_FOLDER);
            archiveFolder.setEnabled(false);
            draftsFolder = (ListPreference) findPreference(PREFERENCE_DRAFTS_FOLDER);
            draftsFolder.setEnabled(false);
            sentFolder = (ListPreference) findPreference(PREFERENCE_SENT_FOLDER);
            sentFolder.setEnabled(false);
            spamFolder = (ListPreference) findPreference(PREFERENCE_SPAM_FOLDER);
            spamFolder.setEnabled(false);
            trashFolder = (ListPreference) findPreference(PREFERENCE_TRASH_FOLDER);
            trashFolder.setEnabled(false);

            if (!isMoveCapable) {
                PreferenceScreen foldersCategory =
                        (PreferenceScreen) findPreference(PREFERENCE_CATEGORY_FOLDERS);
                foldersCategory.removePreference(archiveFolder);
                foldersCategory.removePreference(spamFolder);
                foldersCategory.removePreference(draftsFolder);
                foldersCategory.removePreference(sentFolder);
                foldersCategory.removePreference(trashFolder);
            }
        }

        @Override
        protected void onPostExecute(Void res) {
            initListPreference(autoExpandFolder, account.getAutoExpandFolder(), allFolderLabels, allFolderValues);
            autoExpandFolder.setEnabled(true);
            if (isMoveCapable) {
                initListPreference(archiveFolder, account.getArchiveFolder(), allFolderLabels, allFolderValues);
                initListPreference(draftsFolder, account.getDraftsFolder(), allFolderLabels, allFolderValues);
                initListPreference(sentFolder, account.getSentFolder(), allFolderLabels, allFolderValues);
                initListPreference(spamFolder, account.getSpamFolder(), allFolderLabels, allFolderValues);
                initListPreference(trashFolder, account.getTrashFolder(), allFolderLabels, allFolderValues);
                archiveFolder.setEnabled(true);
                spamFolder.setEnabled(true);
                draftsFolder.setEnabled(true);
                sentFolder.setEnabled(true);
                trashFolder.setEnabled(true);
            }
        }
    }
}
