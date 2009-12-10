
package com.android.email.mail.transport;

import android.util.Log;
import com.android.email.Email;
import com.android.email.PeekableInputStream;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Transport;
import com.android.email.mail.store.WebDavStore;

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
        Log.d(Email.LOG_TAG, ">>> New WebDavTransport creation complete");
    }

    public void open() throws MessagingException
    {
        Log.d(Email.LOG_TAG, ">>> open called on WebDavTransport ");
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
