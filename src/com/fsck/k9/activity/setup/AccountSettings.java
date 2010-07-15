
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;
import android.view.KeyEvent;
import com.fsck.k9.*;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.ChooseIdentity;
import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.activity.ManageIdentities;
import com.fsck.k9.mail.Store;
import com.fsck.k9.service.MailService;

public class AccountSettings extends K9PreferenceActivity
{
    private static final String EXTRA_ACCOUNT = "account";

    private static final int SELECT_AUTO_EXPAND_FOLDER = 1;

    private static final int ACTIVITY_MANAGE_IDENTITIES = 2;

    private static final String PREFERENCE_TOP_CATERGORY = "account_settings";
    private static final String PREFERENCE_DESCRIPTION = "account_description";
    private static final String PREFERENCE_COMPOSITION = "composition";
    private static final String PREFERENCE_MANAGE_IDENTITIES = "manage_identities";
    private static final String PREFERENCE_FREQUENCY = "account_check_frequency";
    private static final String PREFERENCE_DISPLAY_COUNT = "account_display_count";
    private static final String PREFERENCE_DEFAULT = "account_default";
    private static final String PREFERENCE_HIDE_BUTTONS = "hide_buttons_enum";
    private static final String PREFERENCE_HIDE_MOVE_BUTTONS = "hide_move_buttons_enum";
    private static final String PREFERENCE_ENABLE_MOVE_BUTTONS = "enable_move_buttons";
    private static final String PREFERENCE_NOTIFY = "account_notify";
    private static final String PREFERENCE_NOTIFY_SELF = "account_notify_self";
    private static final String PREFERENCE_NOTIFY_SYNC = "account_notify_sync";
    private static final String PREFERENCE_VIBRATE = "account_vibrate";
    private static final String PREFERENCE_VIBRATE_PATTERN = "account_vibrate_pattern";
    private static final String PREFERENCE_VIBRATE_TIMES = "account_vibrate_times";
    private static final String PREFERENCE_RINGTONE = "account_ringtone";
    private static final String PREFERENCE_INCOMING = "incoming";
    private static final String PREFERENCE_OUTGOING = "outgoing";
    private static final String PREFERENCE_DISPLAY_MODE = "folder_display_mode";
    private static final String PREFERENCE_SYNC_MODE = "folder_sync_mode";
    private static final String PREFERENCE_PUSH_MODE = "folder_push_mode";
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
    private static final String PREFERENCE_QUOTE_PREFIX = "account_quote_prefix";
    private static final String PREFERENCE_SYNC_REMOTE_DELETIONS = "account_sync_remote_deletetions";


    private Account mAccount;

    private EditTextPreference mAccountDescription;
    private ListPreference mCheckFrequency;
    private ListPreference mDisplayCount;
    private ListPreference mMessageAge;
    private ListPreference mMessageSize;
    private CheckBoxPreference mAccountDefault;
    private CheckBoxPreference mAccountNotify;
    private CheckBoxPreference mAccountNotifySelf;
    private ListPreference mAccountHideButtons;
    private ListPreference mAccountHideMoveButtons;
    private CheckBoxPreference mAccountEnableMoveButtons;
    private CheckBoxPreference mAccountNotifySync;
    private CheckBoxPreference mAccountVibrate;
    private ListPreference mAccountVibratePattern;
    private EditTextPreference mAccountVibrateTimes;
    private RingtonePreference mAccountRingtone;
    private ListPreference mDisplayMode;
    private ListPreference mSyncMode;
    private ListPreference mPushMode;
    private ListPreference mTargetMode;
    private ListPreference mDeletePolicy;
    private ListPreference mExpungePolicy;
    private ListPreference mSearchableFolders;
    private Preference mAutoExpandFolder;
    private Preference mChipColor;
    private Preference mLedColor;
    private boolean mIncomingChanged = false;
    private CheckBoxPreference mNotificationOpensUnread;
    private EditTextPreference mAccountQuotePrefix;
    private CheckBoxPreference mSyncRemoteDeletions;


    public static void actionSettings(Context context, Account account)
    {
        Intent i = new Intent(context, AccountSettings.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        boolean isPushCapable = false;
        boolean isExpungeCapable = false;
        Store store = null;
        try
        {
            store = mAccount.getRemoteStore();
            isPushCapable = store.isPushCapable();
            isExpungeCapable = store.isExpungeCapable();
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Could not get remote store", e);
        }

        addPreferencesFromResource(R.xml.account_settings_preferences);

        Preference category = findPreference(PREFERENCE_TOP_CATERGORY);
        category.setTitle(getString(R.string.account_settings_title_fmt));

        mAccountDescription = (EditTextPreference) findPreference(PREFERENCE_DESCRIPTION);
        mAccountDescription.setSummary(mAccount.getDescription());
        mAccountDescription.setText(mAccount.getDescription());
        mAccountDescription.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                mAccountDescription.setSummary(summary);
                mAccountDescription.setText(summary);
                return false;
            }
        });

        mAccountQuotePrefix = (EditTextPreference) findPreference(PREFERENCE_QUOTE_PREFIX);
        mAccountQuotePrefix.setSummary(mAccount.getQuotePrefix());
        mAccountQuotePrefix.setText(mAccount.getQuotePrefix());
        mAccountQuotePrefix.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String value = newValue.toString();
                mAccountQuotePrefix.setSummary(value);
                mAccountQuotePrefix.setText(value);
                return false;
            }
        });

        mCheckFrequency = (ListPreference) findPreference(PREFERENCE_FREQUENCY);
        mCheckFrequency.setValue(String.valueOf(mAccount.getAutomaticCheckIntervalMinutes()));
        mCheckFrequency.setSummary(mCheckFrequency.getEntry());
        mCheckFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mCheckFrequency.findIndexOfValue(summary);
                mCheckFrequency.setSummary(mCheckFrequency.getEntries()[index]);
                mCheckFrequency.setValue(summary);
                return false;
            }
        });

        mDisplayMode = (ListPreference) findPreference(PREFERENCE_DISPLAY_MODE);
        mDisplayMode.setValue(mAccount.getFolderDisplayMode().name());
        mDisplayMode.setSummary(mDisplayMode.getEntry());
        mDisplayMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mDisplayMode.findIndexOfValue(summary);
                mDisplayMode.setSummary(mDisplayMode.getEntries()[index]);
                mDisplayMode.setValue(summary);
                return false;
            }
        });

        mSyncMode = (ListPreference) findPreference(PREFERENCE_SYNC_MODE);
        mSyncMode.setValue(mAccount.getFolderSyncMode().name());
        mSyncMode.setSummary(mSyncMode.getEntry());
        mSyncMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mSyncMode.findIndexOfValue(summary);
                mSyncMode.setSummary(mSyncMode.getEntries()[index]);
                mSyncMode.setValue(summary);
                return false;
            }
        });

        mPushMode = (ListPreference) findPreference(PREFERENCE_PUSH_MODE);
        mPushMode.setEnabled(isPushCapable);
        mPushMode.setValue(mAccount.getFolderPushMode().name());
        mPushMode.setSummary(mPushMode.getEntry());
        mPushMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mPushMode.findIndexOfValue(summary);
                mPushMode.setSummary(mPushMode.getEntries()[index]);
                mPushMode.setValue(summary);
                return false;
            }
        });

        mTargetMode = (ListPreference) findPreference(PREFERENCE_TARGET_MODE);
        mTargetMode.setValue(mAccount.getFolderTargetMode().name());
        mTargetMode.setSummary(mTargetMode.getEntry());
        mTargetMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mTargetMode.findIndexOfValue(summary);
                mTargetMode.setSummary(mTargetMode.getEntries()[index]);
                mTargetMode.setValue(summary);
                return false;
            }
        });

        mDeletePolicy = (ListPreference) findPreference(PREFERENCE_DELETE_POLICY);
        mDeletePolicy.setValue("" + mAccount.getDeletePolicy());
        mDeletePolicy.setSummary(mDeletePolicy.getEntry());
        mDeletePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mDeletePolicy.findIndexOfValue(summary);
                mDeletePolicy.setSummary(mDeletePolicy.getEntries()[index]);
                mDeletePolicy.setValue(summary);
                return false;
            }
        });


        mExpungePolicy = (ListPreference) findPreference(PREFERENCE_EXPUNGE_POLICY);
        mExpungePolicy.setEnabled(isExpungeCapable);
        mExpungePolicy.setValue(mAccount.getExpungePolicy());
        mExpungePolicy.setSummary(mExpungePolicy.getEntry());
        mExpungePolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mExpungePolicy.findIndexOfValue(summary);
                mExpungePolicy.setSummary(mExpungePolicy.getEntries()[index]);
                mExpungePolicy.setValue(summary);
                return false;
            }
        });

        mSyncRemoteDeletions = (CheckBoxPreference) findPreference(PREFERENCE_SYNC_REMOTE_DELETIONS);
        mSyncRemoteDeletions.setChecked(mAccount.syncRemoteDeletions());

        mSearchableFolders = (ListPreference) findPreference(PREFERENCE_SEARCHABLE_FOLDERS);
        mSearchableFolders.setValue(mAccount.getSearchableFolders().name());
        mSearchableFolders.setSummary(mSearchableFolders.getEntry());
        mSearchableFolders.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mSearchableFolders.findIndexOfValue(summary);
                mSearchableFolders.setSummary(mSearchableFolders.getEntries()[index]);
                mSearchableFolders.setValue(summary);
                return false;
            }
        });

        mDisplayCount = (ListPreference) findPreference(PREFERENCE_DISPLAY_COUNT);
        mDisplayCount.setValue(String.valueOf(mAccount.getDisplayCount()));
        mDisplayCount.setSummary(mDisplayCount.getEntry());
        mDisplayCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mDisplayCount.findIndexOfValue(summary);
                mDisplayCount.setSummary(mDisplayCount.getEntries()[index]);
                mDisplayCount.setValue(summary);
                return false;
            }
        });

        mMessageAge = (ListPreference) findPreference(PREFERENCE_MESSAGE_AGE);
        mMessageAge.setValue(String.valueOf(mAccount.getMaximumPolledMessageAge()));
        mMessageAge.setSummary(mMessageAge.getEntry());
        mMessageAge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mMessageAge.findIndexOfValue(summary);
                mMessageAge.setSummary(mMessageAge.getEntries()[index]);
                mMessageAge.setValue(summary);
                return false;
            }
        });



        mMessageSize = (ListPreference) findPreference(PREFERENCE_MESSAGE_SIZE);
        mMessageSize.setValue(String.valueOf(mAccount.getMaximumAutoDownloadMessageSize()));
        mMessageSize.setSummary(mMessageSize.getEntry());
        mMessageSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mMessageSize.findIndexOfValue(summary);
                mMessageSize.setSummary(mMessageSize.getEntries()[index]);
                mMessageSize.setValue(summary);
                return false;
            }
        });



        mAccountDefault = (CheckBoxPreference) findPreference(PREFERENCE_DEFAULT);
        mAccountDefault.setChecked(
            mAccount.equals(Preferences.getPreferences(this).getDefaultAccount()));


        mAccountHideButtons = (ListPreference) findPreference(PREFERENCE_HIDE_BUTTONS);
        mAccountHideButtons.setValue("" + mAccount.getHideMessageViewButtons());
        mAccountHideButtons.setSummary(mAccountHideButtons.getEntry());
        mAccountHideButtons.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mAccountHideButtons.findIndexOfValue(summary);
                mAccountHideButtons.setSummary(mAccountHideButtons.getEntries()[index]);
                mAccountHideButtons.setValue(summary);
                return false;
            }
        });

        mAccountEnableMoveButtons = (CheckBoxPreference) findPreference(PREFERENCE_ENABLE_MOVE_BUTTONS);
        mAccountEnableMoveButtons.setChecked(mAccount.getEnableMoveButtons());

        mAccountHideMoveButtons = (ListPreference) findPreference(PREFERENCE_HIDE_MOVE_BUTTONS);
        mAccountHideMoveButtons.setValue("" + mAccount.getHideMessageViewMoveButtons());
        mAccountHideMoveButtons.setSummary(mAccountHideMoveButtons.getEntry());
        mAccountHideMoveButtons.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mAccountHideMoveButtons.findIndexOfValue(summary);
                mAccountHideMoveButtons.setSummary(mAccountHideMoveButtons.getEntries()[index]);
                mAccountHideMoveButtons.setValue(summary);
                return false;
            }
        });

        mAccountNotify = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY);
        mAccountNotify.setChecked(mAccount.isNotifyNewMail());

        mAccountNotifySelf = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY_SELF);
        mAccountNotifySelf.setChecked(mAccount.isNotifySelfNewMail());

        mAccountNotifySync = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY_SYNC);
        mAccountNotifySync.setChecked(mAccount.isShowOngoing());

        mAccountRingtone = (RingtonePreference) findPreference(PREFERENCE_RINGTONE);

        // XXX: The following two lines act as a workaround for the RingtonePreference
        //      which does not let us set/get the value programmatically
        SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        String currentRingtone = (!mAccount.shouldRing() ? null : mAccount.getRingtone());
        prefs.edit().putString(PREFERENCE_RINGTONE, currentRingtone).commit();

        mAccountVibrate = (CheckBoxPreference) findPreference(PREFERENCE_VIBRATE);
        mAccountVibrate.setChecked(mAccount.isVibrate());

        mAccountVibratePattern = (ListPreference) findPreference(PREFERENCE_VIBRATE_PATTERN);
        mAccountVibratePattern.setValue(String.valueOf(mAccount.getVibratePattern()));
        mAccountVibratePattern.setSummary(mAccountVibratePattern.getEntry());
        mAccountVibratePattern.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mAccountVibratePattern.findIndexOfValue(summary);
                mAccountVibratePattern.setSummary(mAccountVibratePattern.getEntries()[index]);
                mAccountVibratePattern.setValue(summary);
                return false;
            }
        });

        mAccountVibrateTimes = (EditTextPreference) findPreference(PREFERENCE_VIBRATE_TIMES);
        mAccountVibrateTimes.setSummary(String.valueOf(mAccount.getVibrateTimes()));
        mAccountVibrateTimes.setText(String.valueOf(mAccount.getVibrateTimes()));
        mAccountVibrateTimes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String value = newValue.toString();
                mAccountVibrateTimes.setSummary(value);
                mAccountVibrateTimes.setText(value);
                return false;
            }
        });

        mNotificationOpensUnread = (CheckBoxPreference)findPreference(PREFERENCE_NOTIFICATION_OPENS_UNREAD);
        mNotificationOpensUnread.setChecked(mAccount.goToUnreadMessageSearch());


        mAutoExpandFolder = (Preference)findPreference(PREFERENCE_AUTO_EXPAND_FOLDER);

        mAutoExpandFolder.setSummary(translateFolder(mAccount.getAutoExpandFolderName()));

        mAutoExpandFolder.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                onChooseAutoExpandFolder();
                return false;
            }
        }
        );


        mChipColor = (Preference)findPreference(PREFERENCE_CHIP_COLOR);

        mChipColor.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                onChooseChipColor();
                return false;
            }
        }
        );

        mLedColor = (Preference)findPreference(PREFERENCE_LED_COLOR);

        mLedColor.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                onChooseLedColor();
                return false;
            }
        }
        );



        findPreference(PREFERENCE_COMPOSITION).setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                onCompositionSettings();
                return true;
            }
        });

        findPreference(PREFERENCE_MANAGE_IDENTITIES).setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                onManageIdentities();
                return true;
            }
        });

        findPreference(PREFERENCE_INCOMING).setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                mIncomingChanged = true;
                onIncomingSettings();
                return true;
            }
        });

        findPreference(PREFERENCE_OUTGOING).setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                onOutgoingSettings();
                return true;
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //mAccount.refresh(Preferences.getPreferences(this));
    }

    private void saveSettings()
    {
        if (mAccountDefault.isChecked())
        {
            Preferences.getPreferences(this).setDefaultAccount(mAccount);
        }

        mAccount.setDescription(mAccountDescription.getText());
        mAccount.setNotifyNewMail(mAccountNotify.isChecked());
        mAccount.setNotifySelfNewMail(mAccountNotifySelf.isChecked());
        mAccount.setShowOngoing(mAccountNotifySync.isChecked());
        mAccount.setDisplayCount(Integer.parseInt(mDisplayCount.getValue()));
        mAccount.setMaximumPolledMessageAge(Integer.parseInt(mMessageAge.getValue()));
        mAccount.setMaximumAutoDownloadMessageSize(Integer.parseInt(mMessageSize.getValue()));
        mAccount.setVibrate(mAccountVibrate.isChecked());
        mAccount.setVibratePattern(Integer.parseInt(mAccountVibratePattern.getValue()));
        mAccount.setVibrateTimes(Integer.parseInt(mAccountVibrateTimes.getText()));
        mAccount.setGoToUnreadMessageSearch(mNotificationOpensUnread.isChecked());
        mAccount.setFolderTargetMode(Account.FolderMode.valueOf(mTargetMode.getValue()));
        mAccount.setDeletePolicy(Integer.parseInt(mDeletePolicy.getValue()));
        mAccount.setExpungePolicy(mExpungePolicy.getValue());
        mAccount.setSyncRemoteDeletions(mSyncRemoteDeletions.isChecked());
        mAccount.setSearchableFolders(Account.Searchable.valueOf(mSearchableFolders.getValue()));
        mAccount.setQuotePrefix(mAccountQuotePrefix.getText());

        boolean needsRefresh = mAccount.setAutomaticCheckIntervalMinutes(Integer.parseInt(mCheckFrequency.getValue()));
        needsRefresh |= mAccount.setFolderSyncMode(Account.FolderMode.valueOf(mSyncMode.getValue()));

        boolean needsPushRestart = mAccount.setFolderPushMode(Account.FolderMode.valueOf(mPushMode.getValue()));
        boolean displayModeChanged = mAccount.setFolderDisplayMode(Account.FolderMode.valueOf(mDisplayMode.getValue()));

        if (mAccount.getFolderPushMode() != FolderMode.NONE)
        {
            needsPushRestart |= displayModeChanged;
            needsPushRestart |= mIncomingChanged;
        }

        SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        String newRingtone = prefs.getString(PREFERENCE_RINGTONE, null);
        if (newRingtone != null)
        {
            mAccount.setRing(true);
            mAccount.setRingtone(newRingtone);
        }
        else
        {
            if (mAccount.shouldRing())
            {
                mAccount.setRingtone(null);
            }
        }

        mAccount.setHideMessageViewButtons(Account.HideButtons.valueOf(mAccountHideButtons.getValue()));
        mAccount.setHideMessageViewMoveButtons(Account.HideButtons.valueOf(mAccountHideMoveButtons.getValue()));
        mAccount.setEnableMoveButtons(mAccountEnableMoveButtons.isChecked());
        mAccount.setAutoExpandFolderName(reverseTranslateFolder(mAutoExpandFolder.getSummary().toString()));
        mAccount.save(Preferences.getPreferences(this));
        if (needsRefresh && needsPushRestart)
        {
            MailService.actionReset(this, null);
        }
        else if (needsRefresh)
        {
            MailService.actionReschedulePoll(this, null);
        }
        else if (needsPushRestart)
        {
            MailService.actionRestartPushers(this, null);
        }
        // TODO: refresh folder list here
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
                case SELECT_AUTO_EXPAND_FOLDER:
                    mAutoExpandFolder.setSummary(translateFolder(data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER)));
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void onCompositionSettings()
    {
        AccountSetupComposition.actionEditCompositionSettings(this, mAccount);
    }

    private void onManageIdentities()
    {
        Intent intent = new Intent(this, ManageIdentities.class);
        intent.putExtra(ChooseIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
        startActivityForResult(intent, ACTIVITY_MANAGE_IDENTITIES);
    }

    private void onIncomingSettings()
    {
        AccountSetupIncoming.actionEditIncomingSettings(this, mAccount);
    }

    private void onOutgoingSettings()
    {
        AccountSetupOutgoing.actionEditOutgoingSettings(this, mAccount);
    }

    public void onChooseChipColor()
    {
        new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener()
        {
            public void colorChanged(int color)
            {
                mAccount.setChipColor(color);
            }
        },
        mAccount.getChipColor()).show();
    }

    public void onChooseLedColor()
    {
        new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener()
        {
            public void colorChanged(int color)
            {
                mAccount.setLedColor(color);
            }
        },
        mAccount.getLedColor()).show();
    }

    public void onChooseAutoExpandFolder()
    {
        Intent selectIntent = new Intent(this, ChooseFolder.class);
        selectIntent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());

        selectIntent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mAutoExpandFolder.getSummary());
        selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_CURRENT, "yes");
        selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_FOLDER_NONE, "yes");
        selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_DISPLAYABLE_ONLY, "yes");
        startActivityForResult(selectIntent, SELECT_AUTO_EXPAND_FOLDER);

    }

    private String translateFolder(String in)
    {

        if (K9.INBOX.equalsIgnoreCase(in))
        {
            return getString(R.string.special_mailbox_name_inbox);
        }
        else
        {
            return in;
        }
    }

    private String reverseTranslateFolder(String in)
    {

        if (getString(R.string.special_mailbox_name_inbox).equals(in))
        {
            return K9.INBOX;
        }
        else
        {
            return in;
        }
    }

}
