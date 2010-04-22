
package com.fsck.k9.mail;

import android.app.Application;

import com.fsck.k9.Account;
import com.fsck.k9.mail.store.ImapStore;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.Pop3Store;
import com.fsck.k9.mail.store.WebDavStore;

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
    /**
     * A global suggestion to Store implementors on how much of the body
     * should be returned on FetchProfile.Item.BODY_SANE requests.
     */
    //Matching MessagingController.MAX_SMALL_MESSAGE_SIZE
    public static final int FETCH_BODY_SANE_SUGGESTED_SIZE = (50 * 1024);

    protected static final int SOCKET_CONNECT_TIMEOUT = 30000;
    protected static final int SOCKET_READ_TIMEOUT = 60000;

    private static HashMap<String, Store> mStores = new HashMap<String, Store>();

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
     */
    public synchronized static LocalStore getLocalInstance(Account account, Application application) throws MessagingException
    {
        String uri = account.getLocalStoreUri();

        if (!uri.startsWith("local"))
        {
            throw new RuntimeException("LocalStore URI doesn't start with 'local'");
        }

        Store store = mStores.get(uri);
        if (store == null)
        {
            store = new LocalStore(account, application);

            if (store != null)
            {
                mStores.put(uri, store);
            }
        }

        if (store == null)
        {
            throw new MessagingException("Unable to locate an applicable Store for " + uri);
        }

        return (LocalStore)store;
    }

    public abstract Folder getFolder(String name) throws MessagingException;

    public abstract List<? extends Folder> getPersonalNamespaces() throws MessagingException;

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
