
package com.fsck.k9.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.thunderbird.core.common.exception.MessagingException;

public interface Body {
    /**
     * Returns the raw data of the body, without transfer encoding etc applied.
     * TODO perhaps it would be better to have an intermediate "simple part" class where this method could reside
     *    because it makes no sense for multiparts
     */
    InputStream getInputStream() throws MessagingException;

    /**
     * Sets the content transfer encoding (7bit, 8bit, quoted-printable or base64).
     */
    void setEncoding(String encoding) throws MessagingException;

    /**
     * Writes the body's data to the given {@link OutputStream}.
     * The written data is transfer encoded (e.g. transformed to Base64 when needed).
     */
    void writeTo(OutputStream out) throws IOException, MessagingException;
}
