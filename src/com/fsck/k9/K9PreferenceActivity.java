package com.fsck.k9;

import android.os.Bundle;
import android.preference.PreferenceActivity;



public class K9PreferenceActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle icicle)
    {
        setTheme(K9.getK9Theme());
        super.onCreate(icicle);
    }


}
