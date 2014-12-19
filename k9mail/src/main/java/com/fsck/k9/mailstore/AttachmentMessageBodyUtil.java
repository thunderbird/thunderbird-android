package com.fsck.k9.mailstore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;

class AttachmentMessageBodyUtil {
    public static void writeTo(BinaryAttachmentBody body, OutputStream out) throws IOException,
            MessagingException {
        InputStream in = body.getInputStream();
        try {
            if (MimeUtil.ENC_7BIT.equalsIgnoreCase(body.getEncoding())) {
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
            in.close();
        }
    }
}
