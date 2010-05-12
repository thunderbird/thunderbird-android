package com.fsck.k9.helper;

import java.lang.reflect.Method;
import android.content.ContentResolver;
import android.content.Context;

public class AutoSyncSdk4 implements IAutoSync
{
    private Method mGetListenForNetworkTickles;
    private Object mContentService;

    public void initialize(Context context) throws NoSuchMethodException
    {
        /*
         * There's no documented/official way to query the state of the
         * auto-sync setting for a normal application in SDK 1.6/API 4.
         *
         * We use reflection to get an ContentService object, so we can call its
         * getListenForNetworkTickles() method. This will return the current
         * auto-sync state.
         */
        try
        {
            Method getContentService = ContentResolver.class.getMethod("getContentService");
            mContentService = getContentService.invoke(null);
            mGetListenForNetworkTickles = mContentService.getClass().getMethod("getListenForNetworkTickles");
        }
        catch (Exception e)
        {
            throw new NoSuchMethodException();
        }
    }

    public boolean getMasterSyncAutomatically()
    {
        try
        {
            return (Boolean) mGetListenForNetworkTickles.invoke(mContentService);
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
