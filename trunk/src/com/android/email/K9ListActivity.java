package com.android.email;

import com.android.email.Preferences;

import android.app.ListActivity;
import android.os.Bundle;


public class K9ListActivity extends ListActivity {
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Preferences.getPreferences(this).getTheme());
        super.onCreate(icicle);
    }


}
