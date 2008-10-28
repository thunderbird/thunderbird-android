
package com.android.email.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.preference.PreferenceActivity;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.R;

public class AccountSettings extends PreferenceActivity {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String PREFERENCE_TOP_CATERGORY = "account_settings";
    private static final String PREFERENCE_DESCRIPTION = "account_description";
    private static final String PREFERENCE_NAME = "account_name";
    private static final String PREFERENCE_FREQUENCY = "account_check_frequency";
    private static final String PREFERENCE_DEFAULT = "account_default";
    private static final String PREFERENCE_NOTIFY = "account_notify";
    private static final String PREFERENCE_VIBRATE = "account_vibrate";
    private static final String PREFERENCE_RINGTONE = "account_ringtone";
    private static final String PREFERENCE_INCOMING = "incoming";
    private static final String PREFERENCE_OUTGOING = "outgoing";
    private static final String PREFERENCE_ADD_ACCOUNT = "add_account";

    private Account mAccount;

    private EditTextPreference mAccountDescription;
    private EditTextPreference mAccountName;
    private ListPreference mCheckFrequency;
    private CheckBoxPreference mAccountDefault;
    private CheckBoxPreference mAccountNotify;
    private CheckBoxPreference mAccountVibrate;
    private RingtonePreference mAccountRingtone;

    public static void actionSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSettings.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccount = (Account)getIntent().getSerializableExtra(EXTRA_ACCOUNT);

        addPreferencesFromResource(R.xml.account_settings_preferences);

        Preference category = findPreference(PREFERENCE_TOP_CATERGORY);
        category.setTitle(getString(R.string.account_settings_title_fmt));

        mAccountDescription = (EditTextPreference) findPreference(PREFERENCE_DESCRIPTION);
        mAccountDescription.setSummary(mAccount.getDescription());
        mAccountDescription.setText(mAccount.getDescription());
        mAccountDescription.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                mAccountDescription.setSummary(summary);
                mAccountDescription.setText(summary);
                return false;
            }
        });

        mAccountName = (EditTextPreference) findPreference(PREFERENCE_NAME);
        mAccountName.setSummary(mAccount.getName());
        mAccountName.setText(mAccount.getName());
        mAccountName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                mAccountName.setSummary(summary);
                mAccountName.setText(summary);
                return false;
            }
        });

        mCheckFrequency = (ListPreference) findPreference(PREFERENCE_FREQUENCY);
        mCheckFrequency.setValue(String.valueOf(mAccount.getAutomaticCheckIntervalMinutes()));
        mCheckFrequency.setSummary(mCheckFrequency.getEntry());
        mCheckFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mCheckFrequency.findIndexOfValue(summary);
                mCheckFrequency.setSummary(mCheckFrequency.getEntries()[index]);
                mCheckFrequency.setValue(summary);
                return false;
            }
        });

        mAccountDefault = (CheckBoxPreference) findPreference(PREFERENCE_DEFAULT);
        mAccountDefault.setChecked(
                mAccount.equals(Preferences.getPreferences(this).getDefaultAccount()));

        mAccountNotify = (CheckBoxPreference) findPreference(PREFERENCE_NOTIFY);
        mAccountNotify.setChecked(mAccount.isNotifyNewMail());

        mAccountRingtone = (RingtonePreference) findPreference(PREFERENCE_RINGTONE);

        // XXX: The following two lines act as a workaround for the RingtonePreference
        //      which does not let us set/get the value programmatically
        SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        prefs.edit().putString(PREFERENCE_RINGTONE, mAccount.getRingtone()).commit();

        mAccountVibrate = (CheckBoxPreference) findPreference(PREFERENCE_VIBRATE);
        mAccountVibrate.setChecked(mAccount.isVibrate());

        findPreference(PREFERENCE_INCOMING).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
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
        findPreference(PREFERENCE_ADD_ACCOUNT).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        onAddNewAccount();
                        return true;
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        mAccount.refresh(Preferences.getPreferences(this));
    }

    private void saveSettings() {
        if (mAccountDefault.isChecked()) {
            Preferences.getPreferences(this).setDefaultAccount(mAccount);
        }
        mAccount.setDescription(mAccountDescription.getText());
        mAccount.setName(mAccountName.getText());
        mAccount.setNotifyNewMail(mAccountNotify.isChecked());
        mAccount.setAutomaticCheckIntervalMinutes(Integer.parseInt(mCheckFrequency.getValue()));
        mAccount.setVibrate(mAccountVibrate.isChecked());
        SharedPreferences prefs = mAccountRingtone.getPreferenceManager().getSharedPreferences();
        mAccount.setRingtone(prefs.getString(PREFERENCE_RINGTONE, null));
        mAccount.save(Preferences.getPreferences(this));
        Email.setServicesEnabled(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void onIncomingSettings() {
        AccountSetupIncoming.actionEditIncomingSettings(this, mAccount);
    }

    private void onOutgoingSettings() {
        AccountSetupOutgoing.actionEditOutgoingSettings(this, mAccount);
    }

    private void onAddNewAccount() {
        AccountSetupBasics.actionNewAccount(this);
        finish();
    }
}
