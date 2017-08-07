
package com.fsck.k9;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import timber.log.Timber;

import com.fsck.k9.mail.Keyword;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.preferences.Storage;

public class Preferences {

    private static Preferences preferences;

    public static synchronized Preferences getPreferences(Context context) {
        Context appContext = context.getApplicationContext();
        if (preferences == null) {
            preferences = new Preferences(appContext);
        }
        return preferences;
    }


    private Storage storage;
    private Map<String, Account> accounts = null;
    private List<Account> accountsInOrder = null;
    private Account newAccount;
    private ArrayList<String> keywordListExternalCodes = null;
    private Context context;

    private Preferences(Context context) {
        storage = Storage.getStorage(context);
        this.context = context;
        if (storage.isEmpty()) {
            Timber.i("Preferences storage is zero-size, importing from Android-style preferences");
            StorageEditor editor = storage.edit();
            editor.copy(context.getSharedPreferences("AndroidMail.Main", Context.MODE_PRIVATE));
            editor.commit();
        }
    }

    public synchronized void loadAccounts() {
        accounts = new HashMap<>();
        accountsInOrder = new LinkedList<>();
        String accountUuids = getStorage().getString("accountUuids", null);
        if ((accountUuids != null) && (accountUuids.length() != 0)) {
            String[] uuids = accountUuids.split(",");
            for (String uuid : uuids) {
                Account newAccount = new Account(this, uuid);
                accounts.put(uuid, newAccount);
                accountsInOrder.add(newAccount);
            }
        }
        if ((newAccount != null) && newAccount.getAccountNumber() != -1) {
            accounts.put(newAccount.getUuid(), newAccount);
            if (!accountsInOrder.contains(newAccount)) {
                accountsInOrder.add(newAccount);
            }
            newAccount = null;
        }
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     *
     * @return all accounts
     */
    public synchronized List<Account> getAccounts() {
        if (accounts == null) {
            loadAccounts();
        }

        return Collections.unmodifiableList(new ArrayList<>(accountsInOrder));
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     *
     * @return all accounts with {@link Account#isAvailable(Context)}
     */
    public synchronized Collection<Account> getAvailableAccounts() {
        List<Account> allAccounts = getAccounts();
        Collection<Account> retval = new ArrayList<>(accounts.size());
        for (Account account : allAccounts) {
            if (account.isEnabled() && account.isAvailable(context)) {
                retval.add(account);
            }
        }

        return retval;
    }

    public synchronized Account getAccount(String uuid) {
        if (accounts == null) {
            loadAccounts();
        }

        return accounts.get(uuid);
    }

    public synchronized Account newAccount() {
        newAccount = new Account(context);
        accounts.put(newAccount.getUuid(), newAccount);
        accountsInOrder.add(newAccount);

        return newAccount;
    }

    public synchronized void deleteAccount(Account account) {
        if (accounts != null) {
            accounts.remove(account.getUuid());
        }
        if (accountsInOrder != null) {
            accountsInOrder.remove(account);
        }

        try {
            RemoteStore.removeInstance(account);
        } catch (Exception e) {
            Timber.e(e, "Failed to reset remote store for account %s", account.getUuid());
        }
        LocalStore.removeAccount(account);

        account.delete(this);

        if (newAccount == account) {
            newAccount = null;
        }
    }

    /**
     * Returns the Account marked as default. If no account is marked as default
     * the first account in the list is marked as default and then returned. If
     * there are no accounts on the system the method returns null.
     */
    public Account getDefaultAccount() {
        String defaultAccountUuid = getStorage().getString("defaultAccountUuid", null);
        Account defaultAccount = getAccount(defaultAccountUuid);

        if (defaultAccount == null) {
            Collection<Account> accounts = getAvailableAccounts();
            if (!accounts.isEmpty()) {
                defaultAccount = accounts.iterator().next();
                setDefaultAccount(defaultAccount);
            }
        }

        return defaultAccount;
    }

    public void setDefaultAccount(Account account) {
        getStorage().edit().putString("defaultAccountUuid", account.getUuid()).commit();
    }

    private synchronized ArrayList<String> loadKeywordList() {
        if (keywordListExternalCodes == null) {
            final String keywordList = getStorage().getString("keywords", null);
            if ((keywordList != null) && (keywordList.length() != 0)) {
                keywordListExternalCodes = new ArrayList<String>(
                    Arrays.asList(keywordList.split(",")));
            } else {
                keywordListExternalCodes = new ArrayList<String>();
            }
        }
        return keywordListExternalCodes;
    }

    private synchronized void saveKeywordList(ArrayList<String> externalCodes)
    {
        String keywordList = new String();
        boolean first = true;
        for (String externalCode : externalCodes) {
            if (first) {
                keywordList += externalCode;
                first = false;
            } else {
                keywordList += "," + externalCode;
            }
        }
        getStorage().edit().putString("keywords", keywordList).commit();
        keywordListExternalCodes = externalCodes;
    }

    public synchronized void loadKeywords() {
        if (keywordListExternalCodes != null) {
            return;
        }

        Storage storage = getStorage();
        ArrayList<String> externalCodes = loadKeywordList();
        for (String externalCode : externalCodes) {
            final String baseKey = "keyword." + externalCode + ".";
            final String name = storage.getString(baseKey + "name", null);
            final boolean visible = storage.getBoolean(baseKey + "visible", true);
            final int color = storage.getInt(baseKey + "color", -1);

            Keyword k = Keyword.getKeywordByExternalCode(externalCode);
            k.setName(name);
            k.setVisible(visible);
            k.setColor(color);
        }
    }

    public synchronized void saveKeywords() {
        saveKeywords(/*saveOnlyKeywordOrder*/ false);
    }

    public synchronized void saveKeywordOrder() {
        saveKeywords(/*saveOnlyKeywordOrder*/ true);
    }

    public synchronized void saveKeywords(boolean saveOnlyKeywordOrder) {
        ArrayList<String> oldExternalCodes = loadKeywordList();
        ArrayList<String> newExternalCodes = new ArrayList<String>();
        HashSet<String> deletedExternalCodes =
            new HashSet<String>(oldExternalCodes);

        for (Keyword keyword : Keyword.getKeywords()) {
            if (!saveOnlyKeywordOrder) {
                updateKeyword(keyword);
            }
            newExternalCodes.add(keyword.getExternalCode());
            deletedExternalCodes.remove(keyword.getExternalCode());
        }
        if (oldExternalCodes != newExternalCodes) {
            saveKeywordList(newExternalCodes);
        }
        if (deletedExternalCodes.size() != 0) {
            deleteKeywords(deletedExternalCodes);
        }
    }

    // NOTE: This either updates in place or appends to the end.
    public synchronized void saveKeyword(Keyword keyword) {
        updateKeyword(keyword);
        ArrayList<String> externalCodes = loadKeywordList();
        boolean addedKeywords = false;
        if (!externalCodes.contains(keyword.getExternalCode())) {
            externalCodes.add(keyword.getExternalCode());
            saveKeywordList(externalCodes);
        }
    }

    private void updateKeyword(Keyword keyword) {
        final String baseKey = "keyword." + keyword.getExternalCode() + ".";
        StorageEditor edit = getStorage().edit();
        edit.putString(baseKey + "name", keyword.getName());
        edit.putBoolean(baseKey + "visible", keyword.isVisible());
        edit.putInt(baseKey + "color", keyword.getColor());
        edit.commit();
    }

    private void deleteKeywords(Collection<String> externalCodes) {
        StorageEditor edit = getStorage().edit();
        for (String externalCode : externalCodes) {
            final String baseKey = "keyword." + externalCode + ".";
            edit.remove(baseKey + "name");
            edit.remove(baseKey + "visible");
            edit.remove(baseKey + "color");
        }
        edit.commit();
    }

    public Storage getStorage() {
        return storage;
    }

    static <T extends Enum<T>> T getEnumStringPref(Storage storage, String key, T defaultEnum) {
        String stringPref = storage.getString(key, null);

        if (stringPref == null) {
            return defaultEnum;
        } else {
            try {
                return Enum.valueOf(defaultEnum.getDeclaringClass(), stringPref);
            } catch (IllegalArgumentException ex) {
                Timber.w(ex, "Unable to convert preference key [%s] value [%s] to enum of type %s",
                        key, stringPref, defaultEnum.getDeclaringClass());

                return defaultEnum;
            }
        }
    }
}
