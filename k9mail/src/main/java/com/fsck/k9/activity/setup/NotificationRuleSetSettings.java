package com.fsck.k9.activity.setup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.widget.Toast;

import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.notification.NotificationRuleSet;

import java.io.ObjectStreamException;

/**
 * Activity to setup notification rule set
 */
public class NotificationRuleSetSettings  extends K9PreferenceActivity {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_RULE_SET_POSITION = "rule_set_position";
 /*
 * Keys of the preferences defined in res/xml/notification_rule_set_settings_preferences.xml
 */

    private static final String PREFERENCE_NOTIFICATION_RULE_SET_NAME = "notification_rule_set_settings_name";
    private static final String PREFERENCE_NOTIFICATION_RULE_SET_SENDER_NAME = "notification_rule_set_settings_sender_name";
    private static final String PREFERENCE_NOTIFICATION_RULE_SET_SENDER_ADDRESS = "notification_rule_set_settings_sender_address";
    private static final String PREFERENCE_NOTIFICATION_RULE_SET_SUBJECT = "notification_rule_set_settings_subject";
    private static final String PREFERENCE_NOTIFICATION_RULE_SET_BODY = "notification_rule_set_settings_body";
    private static final String PREFERENCE_NOTIFICATION_RULE_SET_SHOULD_NOTIFY = "notification_rule_set_settings_should_notify";

    private Account mAccount;
    private boolean mRuleSetChanged;
    private int mRuleSetPosition;
    private EditTextPreference mRuleSetName;
    private EditTextPreference mSenderName;
    private EditTextPreference mSenderAddress;
    private EditTextPreference mSubject;
    private EditTextPreference mBody;
    private CheckBoxPreference mShouldNotify;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.notification_rule_set_settings_preferences);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        mRuleSetPosition = getIntent().getIntExtra(EXTRA_RULE_SET_POSITION, -1);

        NotificationRuleSet ruleSet =null;
        if (mRuleSetPosition == -1) {
            mRuleSetChanged = true;
            ruleSet = new NotificationRuleSet();
        } else {
            mRuleSetChanged = false;
            ruleSet = mAccount.getNotificationSetting().getNotificationRuleSet(mRuleSetPosition);
        }

        mRuleSetName = (EditTextPreference) findPreference(PREFERENCE_NOTIFICATION_RULE_SET_NAME);
        mRuleSetName.setSummary(ruleSet.getName());
        mRuleSetName.setText(ruleSet.getName());
        mRuleSetName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onPreferenceChanged(mRuleSetName, newValue);
                return false;
            }
        });
        mSenderName = (EditTextPreference) findPreference(PREFERENCE_NOTIFICATION_RULE_SET_SENDER_NAME);
        mSenderName.setSummary(ruleSet.getSenderName());
        mSenderName.setText(ruleSet.getSenderName());
        mSenderName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onPreferenceChanged(mSenderName, newValue);
                return false;
            }
        });

        mSenderAddress = (EditTextPreference) findPreference(PREFERENCE_NOTIFICATION_RULE_SET_SENDER_ADDRESS);
        mSenderAddress.setSummary(ruleSet.getSenderAddress());
        mSenderAddress.setText(ruleSet.getSenderAddress());
        mSenderAddress.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!newValue.toString().isEmpty() && !isEmailAddressValid(newValue.toString())) {
                    Toast toast = Toast.makeText(getApplication(), getString(R.string.invalid_email_address), Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    onPreferenceChanged(mSenderAddress, newValue);
                }
                return false;
            }
        });

        mSubject = (EditTextPreference) findPreference(PREFERENCE_NOTIFICATION_RULE_SET_SUBJECT);
        mSubject.setSummary(ruleSet.getSubject());
        mSubject.setText(ruleSet.getSubject());
        mSubject.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onPreferenceChanged(mSubject, newValue);
                return false;
            }
        });


        mBody = (EditTextPreference) findPreference(PREFERENCE_NOTIFICATION_RULE_SET_BODY);
        mBody.setSummary(ruleSet.getBody());
        mBody.setText(ruleSet.getBody());
        mBody.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onPreferenceChanged(mBody, newValue);
                return false;
            }
        });

        mShouldNotify = (CheckBoxPreference)findPreference(PREFERENCE_NOTIFICATION_RULE_SET_SHOULD_NOTIFY);
        mShouldNotify.setChecked(ruleSet.getShouldNotify());
        mShouldNotify.setOnPreferenceClickListener( new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mRuleSetChanged = true;
                return false;
            }
        });
    }

    private void onPreferenceChanged(EditTextPreference editTextPreference, Object newValue) {
        final String summary = newValue.toString();
        editTextPreference.setSummary(summary);
        editTextPreference.setText(summary);
        mRuleSetChanged = true;
    }
    @Override
    public void onBackPressed() {
        if (mSenderName.getText().isEmpty() && mSenderAddress.getText().isEmpty()
                && mSubject.getText().isEmpty() && mBody.getText().isEmpty()) {
            Toast toast = Toast.makeText(getApplication(), getString(R.string.ignoring_empty_notification_rule_set), Toast.LENGTH_LONG);
            toast.show();
        } else {
            saveSettings();
        }
        super.onBackPressed();
    }

    private boolean isEmailAddressValid(String email) {
        return new EmailAddressValidator().isValidAddressOnly(email);
    }
    private void saveSettings() {
        if (mRuleSetChanged) {
            NotificationRuleSet ruleSet = new NotificationRuleSet();
            ruleSet.setName(mRuleSetName.getText());
            ruleSet.setSenderName(mSenderName.getText());
            ruleSet.setSenderAddress(mSenderAddress.getText());
            ruleSet.setSubject(mSubject.getText());
            ruleSet.setBody(mBody.getText());
            ruleSet.setShouldNotify(mShouldNotify.isChecked());
            mAccount.getNotificationSetting().updateNotificationRuleSet(ruleSet, mRuleSetPosition);
            mAccount.save(Preferences.getPreferences(getApplication().getApplicationContext()));
        }
        finish();
    }
}