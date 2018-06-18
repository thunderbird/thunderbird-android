package com.fsck.k9.mail.internet;


public class Rfc2231Value {
    String string;

    public String getString() {
        return string;
    }

    private boolean encoded;

    public boolean isEncoded() {
        return encoded;
    }

    Rfc2231Value(String string, boolean encoded) {
        this.string = string;
        this.encoded = encoded;
    }
}
