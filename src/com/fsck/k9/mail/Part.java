
package com.fsck.k9.mail;

import java.io.IOException;
import java.io.OutputStream;

public interface Part
{
    public void addHeader(String name, String value) throws MessagingException;

    public void removeHeader(String name) throws MessagingException;

    public void setHeader(String name, String value) throws MessagingException;

    public Body getBody() throws MessagingException;

    public String getContentType() throws MessagingException;

    public String getDisposition() throws MessagingException;

    public String getContentId() throws MessagingException;

    public String[] getHeader(String name) throws MessagingException;

    public int getSize() throws MessagingException;

    public boolean isMimeType(String mimeType) throws MessagingException;

    public String getMimeType() throws MessagingException;

    public void setBody(Body body) throws MessagingException;

    public void writeTo(OutputStream out) throws IOException, MessagingException;
}
