package com.fsck.k9.activity;


import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.fsck.k9.ui.R;
import com.fsck.k9.ui.ThemeManager;


public abstract class K9Activity extends AppCompatActivity {
    private final K9ActivityCommon base = new K9ActivityCommon(this, ThemeType.DEFAULT);

    public ThemeManager getThemeManager() {
        return base.getThemeManager();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        base.preOnCreate();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        base.preOnResume();
        super.onResume();
    }

    protected void setLayout(@LayoutRes int layoutResId) {
        setContentView(layoutResId);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            throw new IllegalArgumentException("K9 layouts must provide a toolbar with id='toolbar'.");
        }
        setSupportActionBar(toolbar);
    }
}
