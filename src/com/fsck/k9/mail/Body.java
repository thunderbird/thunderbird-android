
package com.fsck.k9.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Body
{
    public InputStream getInputStream() throws MessagingException;
    public void writeTo(OutputStream out) throws IOException, MessagingException;
}
