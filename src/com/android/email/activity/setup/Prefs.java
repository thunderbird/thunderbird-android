package com.android.email.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceClickListener;

import com.android.email.K9PreferenceActivity;
import com.android.email.Account;
import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.service.MailService;

public class Prefs extends K9PreferenceActivity {

    private static final String PREFERENCE_TOP_CATERGORY = "preferences";
    private static final String PREFERENCE_THEME = "theme";
    private static final String PREFERENCE_BACKGROUND_OPS = "background_ops";
    private static final String PREFERENCE_DEBUG_LOGGING = "debug_logging";
    private static final String PREFERENCE_SENSITIVE_LOGGING = "sensitive_logging";

    private ListPreference mTheme;
    private ListPreference mBackgroundOps;
    private CheckBoxPreference mDebugLogging;
    private CheckBoxPreference mSensitiveLogging;

    private String initBackgroundOps;


    public static void actionPrefs(Context context) {
        Intent i = new Intent(context, Prefs.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addPreferencesFromResource(R.xml.global_preferences);

        mTheme = (ListPreference) findPreference(PREFERENCE_THEME);
        mTheme.setValue(String.valueOf(Email.getK9Theme() == android.R.style.Theme ? "dark" : "light"));
        mTheme.setSummary(mTheme.getEntry());
        mTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mTheme.findIndexOfValue(summary);
                mTheme.setSummary(mTheme.getEntries()[index]);
                mTheme.setValue(summary);
                return false;
            }
        });

        mBackgroundOps = (ListPreference) findPreference(PREFERENCE_BACKGROUND_OPS);
        initBackgroundOps = Email.getBackgroundOps().toString();
        mBackgroundOps.setValue(initBackgroundOps);
        mBackgroundOps.setSummary(mBackgroundOps.getEntry());
        mBackgroundOps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mBackgroundOps.findIndexOfValue(summary);
                mBackgroundOps.setSummary(mBackgroundOps.getEntries()[index]);
                mBackgroundOps.setValue(summary);
                return false;
            }
        });

        mDebugLogging = (CheckBoxPreference)findPreference(PREFERENCE_DEBUG_LOGGING);
        mSensitiveLogging = (CheckBoxPreference)findPreference(PREFERENCE_SENSITIVE_LOGGING);

        mDebugLogging.setChecked(Email.DEBUG);
        mSensitiveLogging.setChecked(Email.DEBUG_SENSITIVE);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void saveSettings() {
        SharedPreferences preferences = Preferences.getPreferences(this).getPreferences();
        Email.setK9Theme(mTheme.getValue().equals("dark") ? android.R.style.Theme : android.R.style.Theme_Light);
        Email.DEBUG = mDebugLogging.isChecked();
        Email.DEBUG_SENSITIVE = mSensitiveLogging.isChecked();
        String newBackgroundOps = mBackgroundOps.getValue();
        Email.setBackgroundOps(newBackgroundOps);
        Email.save(preferences);
        if (newBackgroundOps.equals(initBackgroundOps) == false) {
            MailService.backgroundDataChanged(this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }

}
