
package com.fsck.k9;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Config;
import android.util.Log;
import com.fsck.k9.preferences.Editor;
import com.fsck.k9.preferences.Storage;

public class Preferences
{
    private static Preferences preferences;

    private Storage mStorage;

    private Preferences(Context context)
    {
        mStorage = Storage.getStorage(context);
        if (mStorage.size() == 0)
        {
            Log.i(K9.LOG_TAG, "Preferences storage is zero-size, importing from Android-style preferences");
            Editor editor = mStorage.edit();
            editor.copy(context.getSharedPreferences("AndroidMail.Main", Context.MODE_PRIVATE));
            editor.commit();
        }
    }


    /**
     * TODO need to think about what happens if this gets GCed along with the
     * Activity that initialized it. Do we lose ability to read Preferences in
     * further Activities? Maybe this should be stored in the Application
     * context.
     *
     * @return
     */
    public static synchronized Preferences getPreferences(Context context)
    {
        if (preferences == null)
        {
            preferences = new Preferences(context);
        }
        return preferences;
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     *
     * @return
     */
    public Account[] getAccounts()
    {
        String accountUuids = getPreferences().getString("accountUuids", null);
        if (accountUuids == null || accountUuids.length() == 0)
        {
            return new Account[] {};
        }
        String[] uuids = accountUuids.split(",");
        Account[] accounts = new Account[uuids.length];
        for (int i = 0, length = uuids.length; i < length; i++)
        {
            accounts[i] = new Account(this, uuids[i]);
        }
        return accounts;
    }

    public Account getAccountByContentUri(Uri uri)
    {
        return new Account(this, uri.getPath().substring(1));
    }

    /**
     * Returns the Account marked as default. If no account is marked as default
     * the first account in the list is marked as default and then returned. If
     * there are no accounts on the system the method returns null.
     *
     * @return
     */
    public Account getDefaultAccount()
    {
        String defaultAccountUuid = getPreferences().getString("defaultAccountUuid", null);
        Account defaultAccount = null;
        Account[] accounts = getAccounts();
        if (defaultAccountUuid != null)
        {
            for (Account account : accounts)
            {
                if (account.getUuid().equals(defaultAccountUuid))
                {
                    defaultAccount = account;
                    break;
                }
            }
        }

        if (defaultAccount == null)
        {
            if (accounts.length > 0)
            {
                defaultAccount = accounts[0];
                setDefaultAccount(defaultAccount);
            }
        }

        return defaultAccount;
    }

    public void setDefaultAccount(Account account)
    {
        getPreferences().edit().putString("defaultAccountUuid", account.getUuid()).commit();
    }

    public void dump()
    {
        if (Config.LOGV)
        {
            for (String key : getPreferences().getAll().keySet())
            {
                Log.v(K9.LOG_TAG, key + " = " + getPreferences().getAll().get(key));
            }
        }
    }

    public SharedPreferences getPreferences()
    {
        return mStorage;
    }
}
