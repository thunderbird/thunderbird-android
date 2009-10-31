package com.android.email;

import android.app.ListActivity;
import android.os.Bundle;


public class K9ListActivity extends ListActivity {
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Email.getK9Theme());
        super.onCreate(icicle);
    }


}
