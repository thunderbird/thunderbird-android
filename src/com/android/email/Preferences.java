
package com.android.email;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Config;
import android.util.Log;

public class Preferences {
    private static Preferences preferences;

    SharedPreferences mSharedPreferences;

    private Preferences(Context context) {
        mSharedPreferences = context.getSharedPreferences("AndroidMail.Main", Context.MODE_PRIVATE);
    }

    /**
     * TODO need to think about what happens if this gets GCed along with the
     * Activity that initialized it. Do we lose ability to read Preferences in
     * further Activities? Maybe this should be stored in the Application
     * context.
     *
     * @return
     */
    public static synchronized Preferences getPreferences(Context context) {
        if (preferences == null) {
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
    public Account[] getAccounts() {
        String accountUuids = mSharedPreferences.getString("accountUuids", null);
        if (accountUuids == null || accountUuids.length() == 0) {
            return new Account[] {};
        }
        String[] uuids = accountUuids.split(",");
        Account[] accounts = new Account[uuids.length];
        for (int i = 0, length = uuids.length; i < length; i++) {
            accounts[i] = new Account(this, uuids[i]);
        }
        return accounts;
    }

    public Account getAccountByContentUri(Uri uri) {
        return new Account(this, uri.getPath().substring(1));
    }

    /**
     * Returns the Account marked as default. If no account is marked as default
     * the first account in the list is marked as default and then returned. If
     * there are no accounts on the system the method returns null.
     *
     * @return
     */
    public Account getDefaultAccount() {
        String defaultAccountUuid = mSharedPreferences.getString("defaultAccountUuid", null);
        Account defaultAccount = null;
        Account[] accounts = getAccounts();
        if (defaultAccountUuid != null) {
            for (Account account : accounts) {
                if (account.getUuid().equals(defaultAccountUuid)) {
                    defaultAccount = account;
                    break;
                }
            }
        }

        if (defaultAccount == null) {
            if (accounts.length > 0) {
                defaultAccount = accounts[0];
                setDefaultAccount(defaultAccount);
            }
        }

        return defaultAccount;
    }

    public void setDefaultAccount(Account account) {
        mSharedPreferences.edit().putString("defaultAccountUuid", account.getUuid()).commit();
    }

    public void setEnableDebugLogging(boolean value) {
        mSharedPreferences.edit().putBoolean("enableDebugLogging", value).commit();
    }

    public boolean geteEnableDebugLogging() {
        return mSharedPreferences.getBoolean("enableDebugLogging", false);
    }

    public void setEnableSensitiveLogging(boolean value) {
        mSharedPreferences.edit().putBoolean("enableSensitiveLogging", value).commit();
    }

    public boolean getEnableSensitiveLogging() {
        return mSharedPreferences.getBoolean("enableSensitiveLogging", false);
    }

    public void save() {
    }

    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }

    public void dump() {
        if (Config.LOGV) {
            for (String key : mSharedPreferences.getAll().keySet()) {
                Log.v(Email.LOG_TAG, key + " = " + mSharedPreferences.getAll().get(key));
            }
        }
    }
}
