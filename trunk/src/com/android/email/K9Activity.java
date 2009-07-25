package com.android.email;

import com.android.email.Preferences;

import android.app.Activity;
import android.os.Bundle;


public class K9Activity extends Activity {
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Preferences.getPreferences(this).getTheme());
        super.onCreate(icicle);
    }


}
