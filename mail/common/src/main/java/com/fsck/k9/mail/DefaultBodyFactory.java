package com.fsck.k9.mail;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.BinaryTempFileMessageBody;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;


public class DefaultBodyFactory implements BodyFactory {
    public Body createBody(String contentTransferEncoding, String contentType, InputStream inputStream)
            throws IOException {

        final BinaryTempFileBody tempBody;
        if (MimeUtil.isMessage(contentType)) {
            tempBody = new BinaryTempFileMessageBody(contentTransferEncoding);
        } else {
            tempBody = new BinaryTempFileBody(contentTransferEncoding);
        }

        OutputStream outputStream = tempBody.getOutputStream();
        try {
            copyData(inputStream, outputStream);
        } finally {
            outputStream.close();
        }

        return tempBody;
    }

    protected void copyData(InputStream inputStream, OutputStream outputStream) throws IOException {
        IOUtils.copy(inputStream, outputStream);
    }
}
