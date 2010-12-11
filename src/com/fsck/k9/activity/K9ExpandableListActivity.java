package com.fsck.k9.activity;

import android.app.ExpandableListActivity;
import android.os.Bundle;

import com.fsck.k9.K9;

/**
 * @see ExpandableListActivity
 */
public class K9ExpandableListActivity extends ExpandableListActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setTheme(K9.getK9Theme());
        super.onCreate(savedInstanceState);
    }
}
