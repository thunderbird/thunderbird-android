package com.fsck.k9.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;

public class AutoSyncSdk3 implements IAutoSync
{
    private Method mGetListenForNetworkTickles;
    private Object mQueryMap;

    public void initialize(Context context) throws NoSuchMethodException
    {
        /*
         * There's no documented/official way to query the state of the
         * auto-sync setting for a normal application in SDK 1.5/API 3.
         * 
         * We use reflection to get an Sync.Settings.QueryMap" object, so we
         * can call its getListenForNetworkTickles() method. This will return
         * the current auto-sync state.
         */
        try
        {
            Class<?> clazz = Class.forName("android.provider.Sync$Settings$QueryMap");
            Constructor<?> c = clazz.getConstructor(ContentResolver.class, boolean.class, Handler.class);
            mQueryMap = c.newInstance(context.getContentResolver(), true, null);
            mGetListenForNetworkTickles = mQueryMap.getClass().getMethod("getListenForNetworkTickles");
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
            return (Boolean) mGetListenForNetworkTickles.invoke(mQueryMap);
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
