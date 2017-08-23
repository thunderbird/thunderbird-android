
package com.fsck.k9.mail;

import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.transport.smtp.SmtpTransport;
import com.fsck.k9.mail.transport.WebDavTransport;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public abstract class Transport {

    protected static final int SOCKET_CONNECT_TIMEOUT = 10000;

    // RFC 1047
    protected static final int SOCKET_READ_TIMEOUT = 300000;

    public abstract void open() throws MessagingException;

    public abstract void sendMessage(Message message) throws MessagingException;

    public abstract void close();
}
