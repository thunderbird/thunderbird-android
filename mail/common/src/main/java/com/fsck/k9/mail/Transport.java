
package com.fsck.k9.mail;

public abstract class Transport {
    public abstract void open() throws MessagingException;

    public abstract void sendMessage(Message message) throws MessagingException;

    public abstract void close();
}
