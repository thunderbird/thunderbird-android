package com.fsck.k9.activity;

import com.fsck.k9.K9;
import android.os.Bundle;
import android.preference.PreferenceActivity;



public class K9PreferenceActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle icicle)
    {
        K9Activity.setLanguage(this, K9.getK9Language());
        setTheme(K9.getK9Theme());
        super.onCreate(icicle);
    }


}
