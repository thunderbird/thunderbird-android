
package com.android.email.mail;

import com.android.email.mail.transport.SmtpTransport;

public abstract class Transport {
    protected static final int SOCKET_CONNECT_TIMEOUT = 10000;

    public synchronized static Transport getInstance(String uri) throws MessagingException {
        if (uri.startsWith("smtp")) {
            return new SmtpTransport(uri);
        } else {
            throw new MessagingException("Unable to locate an applicable Transport for " + uri);
        }
    }

    public abstract void open() throws MessagingException;

    public abstract void sendMessage(Message message) throws MessagingException;

    public abstract void close() throws MessagingException;
}
