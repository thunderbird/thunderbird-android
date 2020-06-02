
package com.fsck.k9;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.fsck.k9.backend.BackendManager;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStoreProvider;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.preferences.StoragePersister;
import timber.log.Timber;


public class Preferences {

    private static Preferences preferences;
    private AccountPreferenceSerializer accountPreferenceSerializer;

    public static synchronized Preferences getPreferences(Context context) {
        if (preferences == null) {
            Context appContext = context.getApplicationContext();
            CoreResourceProvider resourceProvider = DI.get(CoreResourceProvider.class);
            LocalKeyStoreManager localKeyStoreManager = DI.get(LocalKeyStoreManager.class);
            AccountPreferenceSerializer accountPreferenceSerializer = DI.get(AccountPreferenceSerializer.class);
            LocalStoreProvider localStoreProvider = DI.get(LocalStoreProvider.class);
            StoragePersister storagePersister = DI.get(StoragePersister.class);

            preferences = new Preferences(appContext, resourceProvider, storagePersister, localStoreProvider, localKeyStoreManager, accountPreferenceSerializer);
        }
        return preferences;
    }

    private Storage storage;
    @GuardedBy("accountLock")
    private Map<String, Account> accounts = null;
    @GuardedBy("accountLock")
    private List<Account> accountsInOrder = null;
    @GuardedBy("accountLock")
    private Account newAccount;

    private final List<AccountsChangeListener> accountsChangeListeners = new CopyOnWriteArrayList<>();
    private final Context context;
    private final LocalStoreProvider localStoreProvider;
    private final CoreResourceProvider resourceProvider;
    private final LocalKeyStoreManager localKeyStoreManager;
    private final StoragePersister storagePersister;

    private final Object accountLock = new Object();

    private Preferences(Context context, CoreResourceProvider resourceProvider,
            StoragePersister storagePersister, LocalStoreProvider localStoreProvider,
            LocalKeyStoreManager localKeyStoreManager,
            AccountPreferenceSerializer accountPreferenceSerializer) {
        this.storage = new Storage();
        this.storagePersister = storagePersister;
        this.context = context;
        this.resourceProvider = resourceProvider;
        this.localStoreProvider = localStoreProvider;
        this.localKeyStoreManager = localKeyStoreManager;
        this.accountPreferenceSerializer = accountPreferenceSerializer;

        Map<String, String> persistedStorageValues = storagePersister.loadValues();
        storage.replaceAll(persistedStorageValues);

        if (storage.isEmpty()) {
            Timber.i("Preferences storage is zero-size, importing from Android-style preferences");
            StorageEditor editor = createStorageEditor();
            editor.copy(context.getSharedPreferences("AndroidMail.Main", Context.MODE_PRIVATE));
            editor.commit();
        }
    }

    public StorageEditor createStorageEditor() {
        return storagePersister.createStorageEditor(storage);
    }

    @RestrictTo(Scope.TESTS)
    public void clearAccounts() {
        synchronized (accountLock) {
            accounts = new HashMap<>();
            accountsInOrder = new LinkedList<>();
        }
    }

    public void loadAccounts() {
        synchronized (accountLock) {
            accounts = new HashMap<>();
            accountsInOrder = new LinkedList<>();
            String accountUuids = getStorage().getString("accountUuids", null);
            if ((accountUuids != null) && (accountUuids.length() != 0)) {
                String[] uuids = accountUuids.split(",");
                for (String uuid : uuids) {
                    Account newAccount = new Account(uuid);
                    accountPreferenceSerializer.loadAccount(newAccount, storage);
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
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     *
     * @return all accounts
     */
    public List<Account> getAccounts() {
        synchronized (accountLock) {
            if (accounts == null) {
                loadAccounts();
            }

            return Collections.unmodifiableList(new ArrayList<>(accountsInOrder));
        }
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     *
     * @return all accounts with {@link Account#isAvailable(Context)}
     */
    public Collection<Account> getAvailableAccounts() {
        List<Account> allAccounts = getAccounts();
        Collection<Account> result = new ArrayList<>(allAccounts.size());
        for (Account account : allAccounts) {
            if (account.isEnabled() && account.isAvailable(context)) {
                result.add(account);
            }
        }

        return result;
    }

    public Account getAccount(String uuid) {
        synchronized (accountLock) {
            if (accounts == null) {
                loadAccounts();
            }

            return accounts.get(uuid);
        }
    }

    public Account newAccount() {
        synchronized (accountLock) {
            String accountUuid = UUID.randomUUID().toString();
            newAccount = new Account(accountUuid);
            accountPreferenceSerializer.loadDefaults(newAccount);
            accounts.put(newAccount.getUuid(), newAccount);
            accountsInOrder.add(newAccount);

            return newAccount;
        }
    }

    public void deleteAccount(Account account) {
        synchronized (accountLock) {
            if (accounts != null) {
                accounts.remove(account.getUuid());
            }
            if (accountsInOrder != null) {
                accountsInOrder.remove(account);
            }

            try {
                getBackendManager().removeBackend(account);
            } catch (Exception e) {
                Timber.e(e, "Failed to reset remote store for account %s", account.getUuid());
            }
            LocalStore.removeAccount(account);

            StorageEditor storageEditor = createStorageEditor();
            accountPreferenceSerializer.delete(storageEditor, storage, account);
            storageEditor.commit();
            localKeyStoreManager.deleteCertificates(account);

            if (newAccount == account) {
                newAccount = null;
            }
        }

        notifyListeners();
    }

    /**
     * Returns the Account marked as default. If no account is marked as default
     * the first account in the list is marked as default and then returned. If
     * there are no accounts on the system the method returns null.
     */
    public Account getDefaultAccount() {
        Account defaultAccount;
        synchronized (accountLock) {
            String defaultAccountUuid = getStorage().getString("defaultAccountUuid", null);
            defaultAccount = getAccount(defaultAccountUuid);
        }

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
        createStorageEditor().putString("defaultAccountUuid", account.getUuid()).commit();
    }

    public Storage getStorage() {
        return storage;
    }

    private BackendManager getBackendManager() {
        return DI.get(BackendManager.class);
    }

    public void saveAccount(Account account) {
        ensureAssignedAccountNumber(account);
        processChangedValues(account);

        StorageEditor editor = createStorageEditor();
        accountPreferenceSerializer.save(editor, storage, account);
        editor.commit();

        notifyListeners();
    }

    private void ensureAssignedAccountNumber(Account account) {
        if (account.getAccountNumber() != Account.UNASSIGNED_ACCOUNT_NUMBER) {
            return;
        }

        int accountNumber = generateAccountNumber();
        account.setAccountNumber(accountNumber);
    }

    private void processChangedValues(Account account) {
        if (account.isChangedVisibleLimits()) {
            try {
                localStoreProvider.getInstance(account).resetVisibleLimits(account.getDisplayCount());
            } catch (MessagingException e) {
                Timber.e(e, "Failed to load LocalStore!");
            }
        }

        account.resetChangeMarkers();
    }

    public int generateAccountNumber() {
        List<Integer> accountNumbers = getExistingAccountNumbers();
        return findNewAccountNumber(accountNumbers);
    }

    private List<Integer> getExistingAccountNumbers() {
        List<Account> accounts = getAccounts();
        List<Integer> accountNumbers = new ArrayList<>(accounts.size());
        for (Account a : accounts) {
            accountNumbers.add(a.getAccountNumber());
        }
        return accountNumbers;
    }

    private static int findNewAccountNumber(List<Integer> accountNumbers) {
        int newAccountNumber = -1;
        Collections.sort(accountNumbers);
        for (int accountNumber : accountNumbers) {
            if (accountNumber > newAccountNumber + 1) {
                break;
            }
            newAccountNumber = accountNumber;
        }
        newAccountNumber++;
        return newAccountNumber;
    }

    public void move(Account account, boolean mUp) {
        synchronized (accountLock) {
            StorageEditor storageEditor = createStorageEditor();
            accountPreferenceSerializer.move(storageEditor, account, storage, mUp);
            storageEditor.commit();
            loadAccounts();
        }

        notifyListeners();
    }

    private void notifyListeners() {
        for (AccountsChangeListener listener : accountsChangeListeners) {
            listener.onAccountsChanged();
        }
    }

    public void addOnAccountsChangeListener(@NonNull AccountsChangeListener accountsChangeListener) {
        accountsChangeListeners.add(accountsChangeListener);
    }

    public void removeOnAccountsChangeListener(@NonNull AccountsChangeListener accountsChangeListener) {
        accountsChangeListeners.remove(accountsChangeListener);
    }
}
