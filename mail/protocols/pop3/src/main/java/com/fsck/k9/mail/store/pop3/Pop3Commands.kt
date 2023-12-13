package com.fsck.k9.mail.store.pop3;


class Pop3Commands {

    static final String STLS_COMMAND = "STLS";
    static final String USER_COMMAND = "USER";
    static final String PASS_COMMAND = "PASS";
    static final String CAPA_COMMAND = "CAPA";
    static final String AUTH_COMMAND = "AUTH";
    static final String STAT_COMMAND = "STAT";
    static final String LIST_COMMAND = "LIST";
    static final String UIDL_COMMAND = "UIDL";
    static final String TOP_COMMAND = "TOP";
    static final String RETR_COMMAND = "RETR";
    static final String DELE_COMMAND = "DELE";
    static final String QUIT_COMMAND = "QUIT";

    static final String STLS_CAPABILITY = "STLS";
    static final String UIDL_CAPABILITY = "UIDL";
    static final String TOP_CAPABILITY = "TOP";
    static final String SASL_CAPABILITY = "SASL";
    static final String AUTH_PLAIN_CAPABILITY = "PLAIN";
    static final String AUTH_CRAM_MD5_CAPABILITY = "CRAM-MD5";
    static final String AUTH_EXTERNAL_CAPABILITY = "EXTERNAL";
}
