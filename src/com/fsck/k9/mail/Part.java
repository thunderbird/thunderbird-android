
package com.fsck.k9.mail;

import java.io.IOException;
import java.io.OutputStream;

public interface Part {
    public void addHeader(String name, String value) throws MessagingException;

    public void removeHeader(String name) throws MessagingException;

    public void setHeader(String name, String value) throws MessagingException;

    public Body getBody();

    public String getContentType() throws MessagingException;

    public String getDisposition() throws MessagingException;

    public String getContentId() throws MessagingException;

    public String[] getHeader(String name) throws MessagingException;

    public int getSize();

    public boolean isMimeType(String mimeType) throws MessagingException;

    public String getMimeType() throws MessagingException;

    public void setBody(Body body) throws MessagingException;

    public void writeTo(OutputStream out) throws IOException, MessagingException;

    /**
     * Called just prior to transmission, once the type of transport is known to
     * be 7bit.
     * <p>
     * All bodies that are 8bit will be converted to 7bit and recursed if of
     * type {@link CompositeBody}, or will be converted to quoted-printable in all other
     * cases. Bodies with encodings other than 8bit remain unchanged.
     *
     * @throws MessagingException
     *
     */
    public abstract void setUsing7bitTransport() throws MessagingException;
}
