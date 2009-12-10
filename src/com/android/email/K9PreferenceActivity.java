package com.android.email;

import android.os.Bundle;
import android.preference.PreferenceActivity;



public class K9PreferenceActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle icicle)
    {
        setTheme(Email.getK9Theme());
        super.onCreate(icicle);
    }


}
