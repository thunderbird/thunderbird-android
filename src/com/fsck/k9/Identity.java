package com.fsck.k9;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fsck.k9.preferences.Storage;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Identity
{
    private static final int IDENTITY_STORAGE_VERSION = 1;

    private final Account mAccount;
    private final String mUuid;
    private String mDescription;
    private String mName;
    private String mEmail;
    private String mSignature;
    private boolean mSignatureUse;

    protected Identity(Account account)
    {
        mAccount = account;
        mUuid = UUID.randomUUID().toString();
    }

    protected Identity(Account account, String uuid, Preferences preferences)
    {
        mAccount = account;
        mUuid = uuid;
        load(preferences);
    }

    public String getUuid()
    {
        return mUuid;
    }
    
    public Account getAccount()
    {
        return mAccount;
    }
    
    public synchronized String getName()
    {
        return mName;
    }

    public synchronized void setName(String name)
    {
        mName = name;
    }

    public synchronized String getEmail()
    {
        return mEmail;
    }

    public synchronized void setEmail(String email)
    {
        mEmail = email;
    }

    public synchronized boolean getSignatureUse()
    {
        return mSignatureUse;
    }

    public synchronized void setSignatureUse(boolean signatureUse)
    {
        mSignatureUse = signatureUse;
    }

    public synchronized String getSignature()
    {
        return mSignature;
    }

    public synchronized void setSignature(String signature)
    {
        mSignature = signature;
    }

    public synchronized String getDescription()
    {
        return mDescription;
    }

    public synchronized void setDescription(String description)
    {
        mDescription = description;
    }

    @Override
    public synchronized String toString()
    {
        return "Account.Identity(description=" + mDescription +
            ", name=" + mName + ", email=" + mEmail + ", signature=" + mSignature;
    }

    /**
     * Upgrades the storage format of identity data.
     * 
     * Code to upgrade the storage format of identity data should go here. When
     * performing upgrades be sure to update all identities of all accounts!
     * 
     * @param prefs Storage object to read and write to the database.
     * @param version Current identity storage version number. Perform your
     *   upgrade if this is lower than the version number you expect. Don't use
     *   IDENTITY_STORAGE_VERSION in comparisions since this constant will
     *   change with future upgrades.
     * 
     * @see IDENTITY_STORAGE_VERSION
     */
    private static synchronized void performStorageUpgrade(SharedPreferences prefs, int version)
    {
        if (version < 1)
        {
            Log.i(K9.LOG_TAG, "Updating account identities to new UUID format");

            String accountUuids = prefs.getString("accountUuids", "");
            String[] uuids = accountUuids.split(",");

            for (int i = 0, length = uuids.length; i < length; i++)
            {
                List<String> identities = new ArrayList<String>();
                String accountUuid = uuids[i];
                int ident = 0;
                boolean gotOne = false;
                do
                {
                    gotOne = false;
                    String name = prefs.getString(accountUuid + ".name." + ident, null);
                    String email = prefs.getString(accountUuid + ".email." + ident, null);
                    boolean signatureUse = prefs.getBoolean(accountUuid + ".signatureUse." + ident, true);
                    String signature = prefs.getString(accountUuid + ".signature." + ident, null);
                    String description = prefs.getString(accountUuid + ".description." + ident, null);

                    if (email != null)
                    {
                        Log.v(K9.LOG_TAG, "Updating account " + accountUuid + ", identity: " + description);

                        gotOne = true;
                        String uuid = UUID.randomUUID().toString();
                        Editor editor = prefs.edit();
                        editor.putString(accountUuid + "." + uuid + ".name", name);
                        editor.putString(accountUuid + "." + uuid + ".email", email);
                        editor.putBoolean(accountUuid + "." + uuid + ".signatureUse", signatureUse);
                        editor.putString(accountUuid + "." + uuid + ".signature", signature);
                        editor.putString(accountUuid + "." + uuid + ".description", description);
                        editor.commit();
                        identities.add(uuid);
                    }

                    Editor editor = prefs.edit();
                    editor.remove(accountUuid + ".name." + ident);
                    editor.remove(accountUuid + ".email." + ident);
                    editor.remove(accountUuid + ".signatureUse." + ident);
                    editor.remove(accountUuid + ".signature." + ident);
                    editor.remove(accountUuid + ".description." + ident);
                    editor.commit();

                    ident++;
                }
                while (gotOne);

                if (identities.size() == 0)
                {
                    String name = prefs.getString(accountUuid + ".name", null);
                    String email = prefs.getString(accountUuid + ".email", null);
                    boolean signatureUse = prefs.getBoolean(accountUuid + ".signatureUse", true);
                    String signature = prefs.getString(accountUuid + ".signature", null);

                    Log.v(K9.LOG_TAG, "Updating account " + accountUuid + ", identity: " + email);

                    String uuid = UUID.randomUUID().toString();
                    Editor editor = prefs.edit();
                    editor.putString(accountUuid + "." + uuid + ".name", name);
                    editor.putString(accountUuid + "." + uuid + ".email", email);
                    editor.putBoolean(accountUuid + "." + uuid + ".signatureUse", signatureUse);
                    editor.putString(accountUuid + "." + uuid + ".signature", signature);
                    editor.putString(accountUuid + "." + uuid + ".description", email);
                    editor.commit();
                    identities.add(uuid);
                }

                Editor editor = prefs.edit();
                editor.remove(accountUuid + ".name");
                editor.remove(accountUuid + ".email");
                editor.remove(accountUuid + ".signatureUse");
                editor.remove(accountUuid + ".signature");

                StringBuffer sb = new StringBuffer();
                for (String uuid : identities)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(',');
                    }
                    sb.append(uuid);
                }
                String identityUuids = sb.toString();
                editor.putString(accountUuid + ".identityUuids", identityUuids);
                editor.commit();
            }
        }

        prefs.edit().putInt("identityVersion", IDENTITY_STORAGE_VERSION).commit();
    }

    /**
     * Load stored settings for this identity.
     * 
     * Please keep this method free of legacy code. Use performStorageUpgrade()
     * for code to read data in an outdated format and convert it to the new
     * storage format.
     */
    private synchronized void load(Preferences preferences)
    {
        Storage prefs = preferences.getPreferences();
        int identityVersion = prefs.getInt("identityVersion", 0);

        if (identityVersion != IDENTITY_STORAGE_VERSION)
        {
            performStorageUpgrade(prefs, identityVersion);
        }

        mName = prefs.getString(mAccount.getUuid() + "." + mUuid + ".name", null);
        mEmail = prefs.getString(mAccount.getUuid() + "." + mUuid + ".email", null);
        mSignatureUse = prefs.getBoolean(mAccount.getUuid() + "." + mUuid  + ".signatureUse", true);
        mSignature = prefs.getString(mAccount.getUuid() + "." + mUuid + ".signature", null);
        mDescription = prefs.getString(mAccount.getUuid() + "." + mUuid + ".description", null);
    }

    public synchronized void save(Preferences preferences)
    {
        if (mEmail == null)
        {
            delete(preferences);
        }

        SharedPreferences prefs = preferences.getPreferences();
        SharedPreferences.Editor editor = prefs.edit();

        String identityUuids = prefs.getString(mAccount.getUuid() + ".identityUuids", "");
        if (!identityUuids.contains(mUuid))
        {
            identityUuids += (identityUuids.length() != 0 ? "," : "") + mUuid;
            editor.putString(mAccount.getUuid() + ".identityUuids", identityUuids);
        }

        editor.putString(mAccount.getUuid() + "." + mUuid + ".name", mName);
        editor.putString(mAccount.getUuid() + "." + mUuid + ".email", mEmail);
        editor.putBoolean(mAccount.getUuid() + "." + mUuid + ".signatureUse", mSignatureUse);
        editor.putString(mAccount.getUuid() + "." + mUuid + ".signature", mSignature);
        editor.putString(mAccount.getUuid() + "." + mUuid + ".description", mDescription);
        editor.commit();
    }

    public synchronized void delete(Preferences preferences)
    {
        SharedPreferences prefs = preferences.getPreferences();
        String[] uuids = prefs.getString(mAccount.getUuid() + ".identityUuids", "").split(",");
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = uuids.length; i < length; i++)
        {
            if (!uuids[i].equals(mUuid))
            {
                if (sb.length() > 0)
                {
                    sb.append(',');
                }
                sb.append(uuids[i]);
            }
        }
        String identityUuids = sb.toString();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(mAccount.getUuid() + ".identityUuids", identityUuids);

        editor.remove(mAccount.getUuid() + "." + mUuid + ".name");
        editor.remove(mAccount.getUuid() + "." + mUuid + ".email");
        editor.remove(mAccount.getUuid() + "." + mUuid + ".signatureUse");
        editor.remove(mAccount.getUuid() + "." + mUuid + ".signature");
        editor.remove(mAccount.getUuid() + "." + mUuid + ".description");
        editor.commit();
    }
}
