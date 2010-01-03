
package com.fsck.k9.mail;

import android.app.Application;
import com.fsck.k9.mail.store.ImapStore;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.Pop3Store;
import com.fsck.k9.mail.store.WebDavStore;

import java.util.HashMap;

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

    protected static final int SOCKET_CONNECT_TIMEOUT = 10000;
    protected static final int SOCKET_READ_TIMEOUT = 60000;

    private static HashMap<String, Store> mStores = new HashMap<String, Store>();

    /**
     * Get an instance of a mail store. The URI is parsed as a standard URI and
     * the scheme is used to determine which protocol will be used. The
     * following schemes are currently recognized: imap - IMAP with no
     * connection security. Ex: imap://username:password@host/ imap+tls - IMAP
     * with TLS connection security, if the server supports it. Ex:
     * imap+tls://username:password@host imap+tls+ - IMAP with required TLS
     * connection security. Connection fails if TLS is not available. Ex:
     * imap+tls+://username:password@host imap+ssl+ - IMAP with required SSL
     * connection security. Connection fails if SSL is not available. Ex:
     * imap+ssl+://username:password@host
     *
     * @param uri The URI of the store.
     * @return
     * @throws MessagingException
     */
    public synchronized static Store getInstance(String uri, Application application) throws MessagingException
    {
        Store store = mStores.get(uri);
        if (store == null)
        {
            if (uri.startsWith("imap"))
            {
                store = new ImapStore(uri);
            }
            else if (uri.startsWith("pop3"))
            {
                store = new Pop3Store(uri);
            }
            else if (uri.startsWith("local"))
            {
                store = new LocalStore(uri, application);
            }
            else if (uri.startsWith("webdav"))
            {
                store = new WebDavStore(uri);
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

    public abstract Folder getFolder(String name) throws MessagingException;

    public abstract Folder[] getPersonalNamespaces() throws MessagingException;

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

}
