package com.fsck.k9.helper;

import com.fsck.k9.K9;
import android.os.Build;
import android.util.Log;

/**
 * Helper class to get the current state of the auto-sync setting.
 */
public class AutoSyncHelper
{
    /**
     * False, if we never tried to load the class for this SDK version.
     * True, otherwise.
     *
     * Note: if sAutoSync is null and sChecked is true, then an error occured
     * while loading the class for the SDK version we're running on.
     */
    private static boolean sChecked = false;

    /**
     * Instance of the SDK specific class that implements the IAutoSync
     * interface.
     */
    private static IAutoSync sAutoSync = null;

    /**
     * String for the auto-sync changed Intent.  This isn't currently exposed by the API
     */
    public static String SYNC_CONN_STATUS_CHANGE = "com.android.sync.SYNC_CONN_STATUS_CHANGED";
    /**
     * Try loading the class that implements IAutoSync for this SDK version.
     *
     * @return the IAutoSync object for this SDK version, or null if something
     *         went wrong.
     */
    private static IAutoSync loadAutoSync()
    {
        /*
         * We're trying to load the class for this SDK version. If anything
         * goes wrong after this point, we don't want to try again.
         */
        sChecked = true;

        /*
         * Check the version of the SDK we are running on. Choose an
         * implementation class designed for that version of the SDK.
         */
        int sdkVersion = Integer.parseInt(Build.VERSION.SDK);

        String className = null;
        if (sdkVersion == Build.VERSION_CODES.CUPCAKE)
        {
            className = "com.fsck.k9.helper.AutoSyncSdk3";
        }
        else if (sdkVersion == Build.VERSION_CODES.DONUT)
        {
            className = "com.fsck.k9.helper.AutoSyncSdk4";
        }
        else if (sdkVersion >= Build.VERSION_CODES.ECLAIR)
        {
            className = "com.fsck.k9.helper.AutoSyncSdk5";
        }

        /*
         * Find the required class by name and instantiate it.
         */
        try
        {
            Class<? extends IAutoSync> clazz =
                Class.forName(className).asSubclass(IAutoSync.class);

            IAutoSync autoSync = clazz.newInstance();
            autoSync.initialize(K9.app);

            return autoSync;
        }
        catch (ClassNotFoundException e)
        {
            Log.e(K9.LOG_TAG, "Couldn't find class: " + className, e);
        }
        catch (InstantiationException e)
        {
            Log.e(K9.LOG_TAG, "Couldn't instantiate class: " + className, e);
        }
        catch (IllegalAccessException e)
        {
            Log.e(K9.LOG_TAG, "Couldn't access class: " + className, e);
        }
        catch (NoSuchMethodException e)
        {
            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "Couldn't load method to get auto-sync state", e);
            }
        }
        return null;
    }

    /**
     * Checks whether we can query the auto-sync state using
     * getMasterSyncAutomatically() or not.
     *
     * @return true, if calls to getMasterSyncAutomatically() will return the
     *         state of the auto-sync setting. false, otherwise.
     */
    public static boolean isAvailable()
    {
        if (!sChecked)
        {
            sAutoSync = loadAutoSync();
        }
        return (sAutoSync != null);
    }

    /**
     * Query the state of the auto-sync setting.
     *
     * @return the state of the auto-sync setting.
     * @see IAutoSync
     */
    public static boolean getMasterSyncAutomatically()
    {
        if (!sChecked)
        {
            sAutoSync = loadAutoSync();
        }

        if (sAutoSync == null)
        {
            throw new RuntimeException(
                "Called getMasterSyncAutomatically() before checking if it's available.");
        }

        return sAutoSync.getMasterSyncAutomatically();
    }
}
