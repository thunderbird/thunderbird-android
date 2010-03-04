
package com.fsck.k9;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Config;
import android.util.Log;
import com.fsck.k9.preferences.Editor;
import com.fsck.k9.preferences.Storage;

public class Preferences
{
    private static Preferences preferences;

    /**
     * TODO need to think about what happens if this gets GCed along with the
     * Activity that initialized it. Do we lose ability to read Preferences in
     * further Activities? Maybe this should be stored in the Application
     * context.
     */
    public static synchronized Preferences getPreferences(Context context)
    {
        if (preferences == null)
        {
            preferences = new Preferences(context);
        }
        return preferences;
    }


    private Storage mStorage;
    private List<Account> accounts;

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

    private synchronized void loadAccounts()
    {
        String accountUuids = getPreferences().getString("accountUuids", null);
        if ((accountUuids != null) && (accountUuids.length() != 0))
        {
            String[] uuids = accountUuids.split(",");
            accounts = new ArrayList<Account>(uuids.length);
            for (int i = 0, length = uuids.length; i < length; i++)
            {
                accounts.add(new Account(this, uuids[i]));
            }
        }
        else
        {
            accounts = new ArrayList<Account>();
        }
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     */
    public synchronized Account[] getAccounts()
    {
        if (accounts == null)
        {
            loadAccounts();
        }

        return accounts.toArray(new Account[0]);
    }

    public synchronized Account getAccount(String uuid)
    {
        if (accounts == null)
        {
            loadAccounts();
        }

        for (Account account : accounts)
        {
            if (account.getUuid().equals(uuid))
            {
                return account;
            }
        }

        return null;
    }

    public synchronized Account newAccount()
    {
        Account account = new Account(K9.app);
        accounts.add(account);

        return account;
    }

    public synchronized void deleteAccount(Account account)
    {
        accounts.remove(account);
        account.delete(this);
    }

    /**
     * Returns the Account marked as default. If no account is marked as default
     * the first account in the list is marked as default and then returned. If
     * there are no accounts on the system the method returns null.
     */
    public Account getDefaultAccount()
    {
        String defaultAccountUuid = getPreferences().getString("defaultAccountUuid", null);
        Account defaultAccount = getAccount(defaultAccountUuid);

        if (defaultAccount == null)
        {
            Account[] accounts = getAccounts();
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
