package com.fsck.k9.mail.internet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;

import com.fsck.k9.mail.CompositeBody;
import com.fsck.k9.mail.MessagingException;

/**
 * A {@link BinaryTempFileBody} extension containing a body of type message/rfc822.
 */
public class BinaryTempFileMessageBody extends BinaryTempFileBody implements CompositeBody {

    public BinaryTempFileMessageBody(String encoding) {
        super(encoding);
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        if (!MimeUtil.ENC_7BIT.equalsIgnoreCase(encoding)
                && !MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)) {
            throw new MessagingException(
                    "Incompatible content-transfer-encoding applied to a CompositeBody");
        }
        mEncoding = encoding;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        InputStream in = getInputStream();
        try {
            if (MimeUtil.ENC_7BIT.equalsIgnoreCase(mEncoding)) {
                /*
                 * If we knew the message was already 7bit clean, then it
                 * could be sent along without processing. But since we
                 * don't know, we recursively parse it.
                 */
                MimeMessage message = new MimeMessage(in, true);
                message.setUsing7bitTransport();
                message.writeTo(out);
            } else {
                IOUtils.copy(in, out);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public void setUsing7bitTransport() throws MessagingException {
        /*
         * There's nothing to recurse into here, so there's nothing to do.
         * The enclosing BodyPart already called setEncoding(MimeUtil.ENC_7BIT).  Once
         * writeTo() is called, the file with the rfc822 body will be opened
         * for reading and will then be recursed.
         */

    }
}
