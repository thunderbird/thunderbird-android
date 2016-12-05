package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.MessagingException;

public class UnsupportedContentTransferEncodingException extends MessagingException {
    public UnsupportedContentTransferEncodingException(String encoding) {
        super("Unsupported encoding: "+encoding);
    }
}
