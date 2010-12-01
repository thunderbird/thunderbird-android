
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

/**
 * Store is the access point for an email message store. It's location can be
 * local or remote and no specific protocol is defined. Store is intended to
 * loosely model in combination the JavaMail classes javax.mail.Store and
 * javax.mail.Folder along with some additional functionality to improve
 * performance on mobile devices. Implementations of this class should focus on
 * making as few network connections as possible.
 */
public abstract class Store
{
    protected static final int SOCKET_CONNECT_TIMEOUT = 30000;
    protected static final int SOCKET_READ_TIMEOUT = 60000;

    /**
     * Remote stores indexed by Uri.
     */
    private static HashMap<String, Store> mStores = new HashMap<String, Store>();
    /**
     * Local stores indexed by UUid because the Uri may change due to migration to/from SD-card.
     */
    private static HashMap<String, Store> mLocalStores = new HashMap<String, Store>();

    protected final Account mAccount;

    protected Store(Account account)
    {
        mAccount = account;
    }

    /**
     * Get an instance of a remote mail store.
     */
    public synchronized static Store getRemoteInstance(Account account) throws MessagingException
    {
        String uri = account.getStoreUri();

        if (uri.startsWith("local"))
        {
            throw new RuntimeException("Asked to get non-local Store object but given LocalStore URI");
        }

        Store store = mStores.get(uri);
        if (store == null)
        {
            if (uri.startsWith("imap"))
            {
                store = new ImapStore(account);
            }
            else if (uri.startsWith("pop3"))
            {
                store = new Pop3Store(account);
            }
            else if (uri.startsWith("webdav"))
            {
                store = new WebDavStore(account);
            }

            if (store != null)
            {
                mStores.put(uri, store);
            }
        }

        if (store == null)
        {
            throw new MessagingException("Unable to locate an applicable Store for " + uri);
        }

        return store;
    }

    /**
     * Get an instance of a local mail store.
     * @throws UnavailableStorageException if not {@link StorageProvider#isReady(Context)}
     */
    public synchronized static LocalStore getLocalInstance(Account account, Application application) throws MessagingException
    {
        Store store = mLocalStores.get(account.getUuid());
        if (store == null)
        {
            store = new LocalStore(account, application);
            mLocalStores.put(account.getUuid(), store);
        }

        return (LocalStore) store;
    }

    public abstract Folder getFolder(String name);

    public abstract List<? extends Folder> getPersonalNamespaces(boolean forceListAll) throws MessagingException;

    public abstract void checkSettings() throws MessagingException;

    public boolean isCopyCapable()
    {
        return false;
    }
    public boolean isMoveCapable()
    {
        return false;
    }
    public boolean isPushCapable()
    {
        return false;
    }
    public boolean isSendCapable()
    {
        return false;
    }
    public boolean isExpungeCapable()
    {
        return false;
    }


    public void sendMessages(Message[] messages) throws MessagingException
    {
    }

    public Pusher getPusher(PushReceiver receiver)
    {
        return null;
    }

    public Account getAccount()
    {
        return mAccount;
    }
}
