
package com.fsck.k9.mail;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.store.ImapStore;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.Pop3Store;
import com.fsck.k9.mail.store.StorageManager.StorageProvider;
import com.fsck.k9.mail.store.UnavailableStorageException;
import com.fsck.k9.mail.store.WebDavStore;

/**
 * Store is the access point for an email message store. It's location can be
 * local or remote and no specific protocol is defined. Store is intended to
 * loosely model in combination the JavaMail classes javax.mail.Store and
 * javax.mail.Folder along with some additional functionality to improve
 * performance on mobile devices. Implementations of this class should focus on
 * making as few network connections as possible.
 */
public abstract class Store {
    protected static final int SOCKET_CONNECT_TIMEOUT = 30000;
    protected static final int SOCKET_READ_TIMEOUT = 60000;

    /**
     * Remote stores indexed by Uri.
     */
    private static HashMap<String, Store> sStores = new HashMap<String, Store>();

    /**
     * Local stores indexed by UUID because the Uri may change due to migration to/from SD-card.
     */
    private static ConcurrentHashMap<String, Store> sLocalStores = new ConcurrentHashMap<String, Store>();

    /**
     * Lock objects indexed by account UUID.
     *
     * @see #getLocalInstance(Account, Application)
     */
    private static ConcurrentHashMap<String, Object> sAccountLocks = new ConcurrentHashMap<String, Object>();

    /**
     * Get an instance of a remote mail store.
     */
    public synchronized static Store getRemoteInstance(Account account) throws MessagingException {
        String uri = account.getStoreUri();

        if (uri.startsWith("local")) {
            throw new RuntimeException("Asked to get non-local Store object but given LocalStore URI");
        }

        Store store = sStores.get(uri);
        if (store == null) {
            if (uri.startsWith("imap")) {
                store = new ImapStore(account);
            } else if (uri.startsWith("pop3")) {
                store = new Pop3Store(account);
            } else if (uri.startsWith("webdav")) {
                store = new WebDavStore(account);
            }

            if (store != null) {
                sStores.put(uri, store);
            }
        }

        if (store == null) {
            throw new MessagingException("Unable to locate an applicable Store for " + uri);
        }

        return store;
    }

    /**
     * Get an instance of a local mail store.
     *
     * @throws UnavailableStorageException
     *          if not {@link StorageProvider#isReady(Context)}
     */
    public static LocalStore getLocalInstance(Account account, Application application)
            throws MessagingException {

        String accountUuid = account.getUuid();

        // Create new per-account lock object if necessary
        sAccountLocks.putIfAbsent(accountUuid, new Object());

        // Get the account's lock object
        Object lock = sAccountLocks.get(accountUuid);

        // Use per-account locks so DatabaseUpgradeService always knows which account database is
        // currently upgraded.
        synchronized (lock) {
            Store store = sLocalStores.get(accountUuid);

            if (store == null) {
                // Creating a LocalStore instance will create or upgrade the database if
                // necessary. This could take some time.
                store = new LocalStore(account, application);

                sLocalStores.put(accountUuid, store);
            }

            return (LocalStore) store;
        }
    }

    public static void removeAccount(Account account) {
        try {
            removeRemoteInstance(account);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Failed to reset remote store for account " + account.getUuid(), e);
        }

        try {
            removeLocalInstance(account);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Failed to reset local store for account " + account.getUuid(), e);
        }
    }

    /**
     * Release reference to a local mail store instance.
     *
     * @param account
     *         {@link Account} instance that is used to get the local mail store instance.
     */
    private static void removeLocalInstance(Account account) {
        String accountUuid = account.getUuid();
        sLocalStores.remove(accountUuid);
    }

    /**
     * Release reference to a remote mail store instance.
     *
     * @param account
     *         {@link Account} instance that is used to get the remote mail store instance.
     */
    private synchronized static void removeRemoteInstance(Account account) {
        String uri = account.getStoreUri();

        if (uri.startsWith("local")) {
            throw new RuntimeException("Asked to get non-local Store object but given " +
                    "LocalStore URI");
        }

        sStores.remove(uri);
    }

    /**
     * Decodes the contents of store-specific URIs and puts them into a {@link ServerSettings}
     * object.
     *
     * @param uri
     *         the store-specific URI to decode
     *
     * @return A {@link ServerSettings} object holding the settings contained in the URI.
     *
     * @see ImapStore#decodeUri(String)
     * @see Pop3Store#decodeUri(String)
     * @see WebDavStore#decodeUri(String)
     */
    public static ServerSettings decodeStoreUri(String uri) {
        if (uri.startsWith("imap")) {
            return ImapStore.decodeUri(uri);
        } else if (uri.startsWith("pop3")) {
            return Pop3Store.decodeUri(uri);
        } else if (uri.startsWith("webdav")) {
            return WebDavStore.decodeUri(uri);
        } else {
            throw new IllegalArgumentException("Not a valid store URI");
        }
    }

    /**
     * Creates a store URI from the information supplied in the {@link ServerSettings} object.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A store URI that holds the same information as the {@code server} parameter.
     *
     * @see ImapStore#createUri(ServerSettings)
     * @see Pop3Store#createUri(ServerSettings)
     * @see WebDavStore#createUri(ServerSettings)
     */
    public static String createStoreUri(ServerSettings server) {
        if (ImapStore.STORE_TYPE.equals(server.type)) {
            return ImapStore.createUri(server);
        } else if (Pop3Store.STORE_TYPE.equals(server.type)) {
            return Pop3Store.createUri(server);
        } else if (WebDavStore.STORE_TYPE.equals(server.type)) {
            return WebDavStore.createUri(server);
        } else {
            throw new IllegalArgumentException("Not a valid store URI");
        }
    }


    protected final Account mAccount;


    protected Store(Account account) {
        mAccount = account;
    }

    public abstract Folder getFolder(String name);

    public abstract List <? extends Folder > getPersonalNamespaces(boolean forceListAll) throws MessagingException;

    public abstract void checkSettings() throws MessagingException;

    public boolean isCopyCapable() {
        return false;
    }

    public boolean isMoveCapable() {
        return false;
    }

    public boolean isPushCapable() {
        return false;
    }

    public boolean isSendCapable() {
        return false;
    }

    public boolean isExpungeCapable() {
        return false;
    }

    public boolean isSeenFlagSupported() {
        return true;
    }

    public void sendMessages(Message[] messages) throws MessagingException {
    }

    public Pusher getPusher(PushReceiver receiver) {
        return null;
    }

    public Account getAccount() {
        return mAccount;
    }
}
