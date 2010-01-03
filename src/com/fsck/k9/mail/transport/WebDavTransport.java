
package com.fsck.k9.mail.transport;

import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.PeekableInputStream;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.WebDavStore;

import java.io.OutputStream;
import java.net.Socket;

public class WebDavTransport extends Transport
{
    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    String host;
    int mPort;
    private int mConnectionSecurity;
    private String mUsername; /* Stores the username for authentications */
    private String mPassword; /* Stores the password for authentications */
    private String mUrl;      /* Stores the base URL for the server */

    boolean mSecure;
    Socket mSocket;
    PeekableInputStream mIn;
    OutputStream mOut;
    private WebDavStore store;

    /**
     * webdav://user:password@server:port CONNECTION_SECURITY_NONE
     * webdav+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * webdav+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * webdav+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * webdav+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     *
     * @param _uri
     */
    public WebDavTransport(String _uri) throws MessagingException
    {
        store = new WebDavStore(_uri);
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, ">>> New WebDavTransport creation complete");
    }

    public void open() throws MessagingException
    {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, ">>> open called on WebDavTransport ");

        store.getHttpClient();
    }

    public void close()
    {
    }

    public void sendMessage(Message message) throws MessagingException
    {

        store.sendMessages(new Message[] { message });


    }

}
