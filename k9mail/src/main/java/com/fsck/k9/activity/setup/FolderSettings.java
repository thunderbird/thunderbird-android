
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import timber.log.Timber;
import com.fsck.k9.*;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderClass;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.service.MailService;

public class FolderSettings extends K9PreferenceActivity {

    private static final String EXTRA_FOLDER_NAME = "com.fsck.k9.folderName";
    private static final String EXTRA_ACCOUNT = "com.fsck.k9.account";

    private static final String PREFERENCE_TOP_CATEGORY = "folder_settings";
    private static final String PREFERENCE_DISPLAY_CLASS = "folder_settings_folder_display_mode";
    private static final String PREFERENCE_SYNC_CLASS = "folder_settings_folder_sync_mode";
    private static final String PREFERENCE_PUSH_CLASS = "folder_settings_folder_push_mode";
    private static final String PREFERENCE_NOTIFY_CLASS = "folder_settings_folder_notify_mode";
    private static final String PREFERENCE_IN_TOP_GROUP = "folder_settings_in_top_group";
    private static final String PREFERENCE_INTEGRATE = "folder_settings_include_in_integrated_inbox";

    private LocalFolder folder;

    private CheckBoxPreference inTopGroup;
    private CheckBoxPreference integrate;
    private ListPreference displayClass;
    private ListPreference syncClass;
    private ListPreference pushClass;
    private ListPreference notifyClass;

    public static void actionSettings(Context context, Account account, String folderName) {
        Intent i = new Intent(context, FolderSettings.class);
        i.putExtra(EXTRA_FOLDER_NAME, folderName);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String folderName = (String)getIntent().getSerializableExtra(EXTRA_FOLDER_NAME);
        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        Account mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        try {
            LocalStore localStore = mAccount.getLocalStore();
            folder = localStore.getFolder(folderName);
            folder.open(Folder.OPEN_MODE_RW);
        } catch (MessagingException me) {
            Timber.e(me, "Unable to edit folder %s preferences", folderName);
            return;
        }

        boolean isPushCapable = false;
        try {
            Store store = mAccount.getRemoteStore();
            isPushCapable = store.isPushCapable();
        } catch (Exception e) {
            Timber.e(e, "Could not get remote store");
        }

        addPreferencesFromResource(R.xml.folder_settings_preferences);

        String displayName = FolderInfoHolder.getDisplayName(this, mAccount, folder.getName());
        Preference category = findPreference(PREFERENCE_TOP_CATEGORY);
        category.setTitle(displayName);


        inTopGroup = (CheckBoxPreference)findPreference(PREFERENCE_IN_TOP_GROUP);
        inTopGroup.setChecked(folder.isInTopGroup());
        integrate = (CheckBoxPreference)findPreference(PREFERENCE_INTEGRATE);
        integrate.setChecked(folder.isIntegrate());

        displayClass = (ListPreference) findPreference(PREFERENCE_DISPLAY_CLASS);
        displayClass.setValue(folder.getDisplayClass().name());
        displayClass.setSummary(displayClass.getEntry());
        displayClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = displayClass.findIndexOfValue(summary);
                displayClass.setSummary(displayClass.getEntries()[index]);
                displayClass.setValue(summary);
                return false;
            }
        });

        syncClass = (ListPreference) findPreference(PREFERENCE_SYNC_CLASS);
        syncClass.setValue(folder.getRawSyncClass().name());
        syncClass.setSummary(syncClass.getEntry());
        syncClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = syncClass.findIndexOfValue(summary);
                syncClass.setSummary(syncClass.getEntries()[index]);
                syncClass.setValue(summary);
                return false;
            }
        });

        pushClass = (ListPreference) findPreference(PREFERENCE_PUSH_CLASS);
        pushClass.setEnabled(isPushCapable);
        pushClass.setValue(folder.getRawPushClass().name());
        pushClass.setSummary(pushClass.getEntry());
        pushClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = pushClass.findIndexOfValue(summary);
                pushClass.setSummary(pushClass.getEntries()[index]);
                pushClass.setValue(summary);
                return false;
            }
        });

        notifyClass = (ListPreference) findPreference(PREFERENCE_NOTIFY_CLASS);
        notifyClass.setValue(folder.getRawNotifyClass().name());
        notifyClass.setSummary(notifyClass.getEntry());
        notifyClass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = notifyClass.findIndexOfValue(summary);
                notifyClass.setSummary(notifyClass.getEntries()[index]);
                notifyClass.setValue(summary);
                return false;
            }
        });
    }

    private void saveSettings() throws MessagingException {
        folder.setInTopGroup(inTopGroup.isChecked());
        folder.setIntegrate(integrate.isChecked());
        // We call getPushClass() because display class changes can affect push class when push class is set to inherit
        FolderClass oldPushClass = folder.getPushClass();
        FolderClass oldDisplayClass = folder.getDisplayClass();
        folder.setDisplayClass(FolderClass.valueOf(displayClass.getValue()));
        folder.setSyncClass(FolderClass.valueOf(syncClass.getValue()));
        folder.setPushClass(FolderClass.valueOf(pushClass.getValue()));
        folder.setNotifyClass(FolderClass.valueOf(notifyClass.getValue()));

        folder.save();

        FolderClass newPushClass = folder.getPushClass();
        FolderClass newDisplayClass = folder.getDisplayClass();

        if (oldPushClass != newPushClass
                || (newPushClass != FolderClass.NO_CLASS && oldDisplayClass != newDisplayClass)) {
            MailService.actionRestartPushers(getApplication(), null);
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
}
