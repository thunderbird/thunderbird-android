package com.fsck.k9.helper;

import android.content.ContentResolver;
import android.content.Context;

public class AutoSyncSdk5 implements IAutoSync
{
    public void initialize(Context context) throws NoSuchMethodException
    {
        // Nothing to do here
    }

    public boolean getMasterSyncAutomatically()
    {
        /*
         * SDK 2.0/API 5 introduced an official method to query the auto-sync
         * state.
         */
        return ContentResolver.getMasterSyncAutomatically();
    }
}
