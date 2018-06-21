package com.fsck.k9.mailstore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64OutputStream;

/**
 * Superclass for attachments that contain binary data.
 * The source for the data differs for the subclasses.
 */
abstract class BinaryAttachmentBody implements Body {
    protected String mEncoding;

    @Override
    public abstract InputStream getInputStream();

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        InputStream in = getInputStream();
        try {
            boolean closeStream = false;
            if (MimeUtil.isBase64Encoding(mEncoding)) {
                out = new Base64OutputStream(out);
                closeStream = true;
            } else if (MimeUtil.isQuotedPrintableEncoded(mEncoding)){
                out = new QuotedPrintableOutputStream(out, false);
                closeStream = true;
            }

            try {
                IOUtils.copy(in, out);
            } finally {
                if (closeStream) {
                    out.close();
                }
            }
        } finally {
            in.close();
        }
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        mEncoding = encoding;
    }

    public String getEncoding() {
        return mEncoding;
    }
}
