package com.fsck.k9.mail.store.pop3;


class Pop3Capabilities {
    boolean cramMD5;
    boolean authPlain;
    boolean stls;
    boolean top;
    boolean uidl;
    boolean external;

    @Override
    public String toString() {
        return String.format("CRAM-MD5 %b, PLAIN %b, STLS %b, TOP %b, UIDL %b, EXTERNAL %b",
             cramMD5,
             authPlain,
             stls,
             top,
             uidl,
             external);
    }
}
