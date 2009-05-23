package com.android.email.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.preference.PreferenceActivity;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceClickListener;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.R;

public class Prefs extends PreferenceActivity {

    private static final String PREFERENCE_TOP_CATERGORY = "preferences";
    private static final String PREFERENCE_THEME = "theme";

    private ListPreference mTheme;

    public static void actionPrefs(Context context) {
        Intent i = new Intent(context, Prefs.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addPreferencesFromResource(R.xml.global_preferences);

        Preference category = findPreference(PREFERENCE_TOP_CATERGORY);


        mTheme = (ListPreference) findPreference(PREFERENCE_THEME);
        mTheme.setValue(String.valueOf(Preferences.getPreferences(this).getTheme() == android.R.style.Theme ? "dark" : "light"));
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
        
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void saveSettings() {
        Preferences.getPreferences(this).setTheme(mTheme.getValue().equals("dark") ? android.R.style.Theme : android.R.style.Theme_Light);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }

}
