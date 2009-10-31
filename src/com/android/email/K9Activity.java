package com.android.email;


import android.app.Activity;
import android.os.Bundle;


public class K9Activity extends Activity {
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Email.getK9Theme());
        super.onCreate(icicle);
    }


}
