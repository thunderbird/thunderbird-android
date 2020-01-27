
package com.fsck.k9.activity.setup;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;

import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.Preferences;
import com.fsck.k9.mailstore.Folder;
import com.fsck.k9.mailstore.FolderType;
import com.fsck.k9.mailstore.LocalStoreProvider;
import com.fsck.k9.ui.R;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.job.K9JobManager;
import com.fsck.k9.mail.FolderClass;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.ui.folders.FolderNameFormatter;
import com.fsck.k9.ui.folders.FolderNameFormatterFactory;

import timber.log.Timber;

public class FolderSettings extends K9PreferenceActivity {

    private static final String EXTRA_FOLDER_NAME = "com.fsck.k9.folderName";
    private static final String EXTRA_ACCOUNT = "com.fsck.k9.account";

    private static final String PREFERENCE_TOP_CATERGORY = "folder_settings";
    private static final String PREFERENCE_DISPLAY_CLASS = "folder_settings_folder_display_mode";
    private static final String PREFERENCE_SYNC_CLASS = "folder_settings_folder_sync_mode";
    private static final String PREFERENCE_PUSH_CLASS = "folder_settings_folder_push_mode";
    private static final String PREFERENCE_NOTIFY_CLASS = "folder_settings_folder_notify_mode";
    private static final String PREFERENCE_IN_TOP_GROUP = "folder_settings_in_top_group";
    private static final String PREFERENCE_INTEGRATE = "folder_settings_include_in_integrated_inbox";

    private final MessagingController messagingController = DI.get(MessagingController.class);
    private final K9JobManager jobManager = DI.get(K9JobManager.class);
    private final FolderNameFormatterFactory folderNameFormatterFactory = DI.get(FolderNameFormatterFactory.class);

    private FolderNameFormatter folderNameFormatter;

    private LocalFolder mFolder;

    private CheckBoxPreference mInTopGroup;
    private CheckBoxPreference mIntegrate;
    private ListPreference mDisplayClass;
    private ListPreference mSyncClass;
    private ListPreference mPushClass;
    private ListPreference mNotifyClass;

    public static void actionSettings(Context context, Account account, String folderServerId) {
        Intent i = new Intent(context, FolderSettings.class);
        i.putExtra(EXTRA_FOLDER_NAME, folderServerId);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        folderNameFormatter = folderNameFormatterFactory.create(this);

        String folderServerId = (String)getIntent().getSerializableExtra(EXTRA_FOLDER_NAME);
        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        Account mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        try {
            LocalStore localStore = DI.get(LocalStoreProvider.class).getInstance(mAccount);
            mFolder = localStore.getFolder(folderServerId);
            mFolder.open();
        } catch (MessagingException me) {
            Timber.e(me, "Unable to edit folder %s preferences", folderServerId);
            return;
        }

        boolean isPushCapable = messagingController.isPushCapable(mAccount);

        addPreferencesFromResource(R.xml.folder_settings_preferences);

        String folderName = mFolder.getName();
        String displayName = getDisplayName(mAccount, folderServerId, folderName);
        Preference category = findPreference(PREFERENCE_TOP_CATERGORY);
        category.setTitle(displayName);


        mInTopGroup = (CheckBoxPreference)findPreference(PREFERENCE_IN_TOP_GROUP);
        mInTopGroup.setChecked(mFolder.isInTopGroup());
        mIntegrate = (CheckBoxPreference)findPreference(PREFERENCE_INTEGRATE);
        mIntegrate.setChecked(mFolder.isIntegrate());

        mDisplayClass = (ListPreference) findPreference(PREFERENCE_DISPLAY_CLASS);
        mDisplayClass.setValue(mFolder.getDisplayClass().name());
        mDisplayClass.setSummary(mDisplayClass.getEntry());
        mDisplayClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mDisplayClass.findIndexOfValue(summary);
                mDisplayClass.setSummary(mDisplayClass.getEntries()[index]);
                mDisplayClass.setValue(summary);
                return false;
            }
        });

        mSyncClass = (ListPreference) findPreference(PREFERENCE_SYNC_CLASS);
        mSyncClass.setValue(mFolder.getRawSyncClass().name());
        mSyncClass.setSummary(mSyncClass.getEntry());
        mSyncClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mSyncClass.findIndexOfValue(summary);
                mSyncClass.setSummary(mSyncClass.getEntries()[index]);
                mSyncClass.setValue(summary);
                return false;
            }
        });

        /* Temporarily disabled. See GH-4253
        mPushClass = (ListPreference) findPreference(PREFERENCE_PUSH_CLASS);
        mPushClass.setEnabled(isPushCapable);
        mPushClass.setValue(mFolder.getRawPushClass().name());
        mPushClass.setSummary(mPushClass.getEntry());
        mPushClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mPushClass.findIndexOfValue(summary);
                mPushClass.setSummary(mPushClass.getEntries()[index]);
                mPushClass.setValue(summary);
                return false;
            }
        });
         */

        mNotifyClass = (ListPreference) findPreference(PREFERENCE_NOTIFY_CLASS);
        mNotifyClass.setValue(mFolder.getRawNotifyClass().name());
        mNotifyClass.setSummary(mNotifyClass.getEntry());
        mNotifyClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mNotifyClass.findIndexOfValue(summary);
                mNotifyClass.setSummary(mNotifyClass.getEntries()[index]);
                mNotifyClass.setValue(summary);
                return false;
            }
        });
    }

    private void saveSettings() throws MessagingException {
        mFolder.setInTopGroup(mInTopGroup.isChecked());
        mFolder.setIntegrate(mIntegrate.isChecked());
        // We call getPushClass() because display class changes can affect push class when push class is set to inherit
        FolderClass oldPushClass = mFolder.getPushClass();
        FolderClass oldDisplayClass = mFolder.getDisplayClass();
        mFolder.setDisplayClass(FolderClass.valueOf(mDisplayClass.getValue()));
        mFolder.setSyncClass(FolderClass.valueOf(mSyncClass.getValue()));
        /* Temporarily disabled. See GH-4253
        mFolder.setPushClass(FolderClass.valueOf(mPushClass.getValue()));
         */
        mFolder.setNotifyClass(FolderClass.valueOf(mNotifyClass.getValue()));

        mFolder.save();

        FolderClass newPushClass = mFolder.getPushClass();
        FolderClass newDisplayClass = mFolder.getDisplayClass();

        if (oldPushClass != newPushClass
                || (newPushClass != FolderClass.NO_CLASS && oldDisplayClass != newDisplayClass)) {
            jobManager.schedulePusherRefresh();
        }
    }

    @Override
    public void onPause() {
        try {
            saveSettings();
        } catch (MessagingException e) {
            Timber.e(e, "Saving folder settings failed");
        }

        super.onPause();
    }

    public String getDisplayName(Account account, String serverId, String name) {
        FolderType folderType = FolderInfoHolder.getFolderType(account, serverId);
        Folder folder = new Folder(-1, serverId, name, folderType);

        return folderNameFormatter.displayName(folder);
    }
}
