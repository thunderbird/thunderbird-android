
package com.fsck.k9.mail;

import android.app.Application;
import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.mail.store.ImapStore;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.Pop3Store;
import com.fsck.k9.mail.store.WebDavStore;
import com.fsck.k9.mail.store.StorageManager.StorageProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Local stores indexed by UUid because the Uri may change due to migration to/from SD-card.
     */
    private static HashMap<String, Store> sLocalStores = new HashMap<String, Store>();


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
     * @throws UnavailableStorageException if not {@link StorageProvider#isReady(Context)}
     */
    public synchronized static LocalStore getLocalInstance(Account account, Application application) throws MessagingException {
        Store store = sLocalStores.get(account.getUuid());
        if (store == null) {
            store = new LocalStore(account, application);
            sLocalStores.put(account.getUuid(), store);
        }

        return (LocalStore) store;
    }

    /**
     * Decodes the contents of store-specific URIs and puts them into a {@link StoreSettings}
     * object.
     *
     * @param uri
     *         the store-specific URI to decode
     *
     * @return A {@link StoreSettings} object holding the settings contained in the URI.
     *
     * @see ImapStore#decodeUri(String)
     * @see Pop3Store#decodeUri(String)
     * @see WebDavStore#decodeUri(String)
     */
    public static StoreSettings decodeStoreUri(String uri) {
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
     * This is an abstraction to get rid of the store-specific URIs.
     *
     * <p>
     * Right now it's only used for settings import/export. But the goal is to get rid of
     * store URIs altogether.
     * </p>
     *
     * @see Account#getStoreUri()
     */
    public static class StoreSettings {
        /**
         * The host name of the incoming server.
         */
        public final String host;

        /**
         * The port number of the incoming server.
         */
        public final int port;

        /**
         * The type of connection security to be used when connecting to the incoming server.
         *
         * {@link ConnectionSecurity#NONE} if not applicable for the store.
         */
        public final ConnectionSecurity connectionSecurity;

        /**
         * The authentication method to use when connecting to the incoming server.
         *
         * {@code null} if not applicable for the store.
         */
        public final String authenticationType;

        /**
         * The username part of the credentials needed to authenticate to the incoming server.
         *
         * {@code null} if unused or not applicable for the store.
         */
        public final String username;

        /**
         * The password part of the credentials needed to authenticate to the incoming server.
         *
         * {@code null} if unused or not applicable for the store.
         */
        public final String password;


        /**
         * Creates a new {@code StoreSettings} object.
         *
         * @param host
         *         see {@link StoreSettings#host}
         * @param port
         *         see {@link StoreSettings#port}
         * @param connectionSecurity
         *         see {@link StoreSettings#connectionSecurity}
         * @param authenticationType
         *         see {@link StoreSettings#authenticationType}
         * @param username
         *         see {@link StoreSettings#username}
         * @param password
         *         see {@link StoreSettings#password}
         */
        public StoreSettings(String host, int port, ConnectionSecurity connectionSecurity,
                String authenticationType, String username, String password) {
            this.host = host;
            this.port = port;
            this.connectionSecurity = connectionSecurity;
            this.authenticationType = authenticationType;
            this.username = username;
            this.password = password;
        }

        /**
         * Returns store-specific settings as key/value pair.
         *
         * <p>Classes that inherit from this one are expected to override this method.</p>
         */
        public Map<String, String> getExtra() {
            return null;
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

    public void sendMessages(Message[] messages) throws MessagingException {
    }

    public Pusher getPusher(PushReceiver receiver) {
        return null;
    }

    public Account getAccount() {
        return mAccount;
    }
}
