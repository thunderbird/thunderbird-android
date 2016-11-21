package com.fsck.k9.mail.internet;

public class UnsupportedEncodingException extends Exception {
    public UnsupportedEncodingException(String encoding) {
        super("Unsupported encoding: "+encoding);
    }
}
