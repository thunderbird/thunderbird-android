package com.fsck.k9.mailstore;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.james.mime4j.util.MimeUtil;

import android.content.Context;
import android.net.Uri;

import com.fsck.k9.mail.CompositeBody;
import com.fsck.k9.mail.MessagingException;

/**
 * A {@link LocalAttachmentBody} extension containing a message/rfc822 type body
 *
 */
class LocalAttachmentMessageBody extends LocalAttachmentBody implements CompositeBody {

    public LocalAttachmentMessageBody(Uri uri, Context context) {
        super(uri, context);
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        AttachmentMessageBodyUtil.writeTo(this, out);
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

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        if (!MimeUtil.ENC_7BIT.equalsIgnoreCase(encoding)
                && !MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)) {
            throw new MessagingException(
                    "Incompatible content-transfer-encoding applied to a CompositeBody");
        }
        mEncoding = encoding;
    }
}
