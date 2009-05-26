package com.android.email;

import com.android.email.Preferences;

import android.preference.PreferenceActivity;
import android.os.Bundle;



public class K9PreferenceActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Preferences.getPreferences(this).getTheme());
        super.onCreate(icicle);
    }


}
