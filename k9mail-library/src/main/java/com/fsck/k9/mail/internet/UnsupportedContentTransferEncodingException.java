package com.fsck.k9.mail.internet;

public class UnsupportedContentTransferEncodingException extends Exception {
    public UnsupportedContentTransferEncodingException(String encoding) {
        super("Unsupported encoding: "+encoding);
    }
}
