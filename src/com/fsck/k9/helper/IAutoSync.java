package com.fsck.k9.helper;

import android.content.Context;

/**
 * Classes that implement this interface know how to query the system for the
 * current state of the auto-sync setting. This method differs from SDK 3 to
 * SDK 5, so there are specialized implementations for each SDK version. 
 */
public interface IAutoSync
{
    /**
     * Do the necessary reflection magic to get the necessary objects and/or
     * methods to later query the state of the auto-sync setting.
     * 
     * @param context The application context object.
     * @throws NoSuchMethodException if something went wrong. 
     */
    public void initialize(Context context) throws NoSuchMethodException;

    /**
     * Query the state of the auto-sync setting.
     * 
     * @return the state of the auto-sync setting.
     */
    public boolean getMasterSyncAutomatically();
}
