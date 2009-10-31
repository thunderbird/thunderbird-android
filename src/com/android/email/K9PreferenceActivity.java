package com.android.email;

import android.preference.PreferenceActivity;
import android.os.Bundle;



public class K9PreferenceActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Email.getK9Theme());
        super.onCreate(icicle);
    }


}
