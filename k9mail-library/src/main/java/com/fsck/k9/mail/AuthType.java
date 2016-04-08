package com.fsck.k9.mail;

public enum AuthType {
    /*
     * The names of these authentication types are saved as strings when
     * settings are exported and are also saved as part of the Server URI stored
     * in the account settings.
     */
    PLAIN,
    CRAM_MD5,
    SCRAM_SHA1,
    EXTERNAL,

    XOAUTH,
    /**
     * XOAUTH2 is an OAuth2.0 protocol designed/used by GMail.
     *
     * https://developers.google.com/gmail/xoauth2_protocol#the_sasl_xoauth2_mechanism
     */
    XOAUTH2,

    /*
     * The following are obsolete authentication settings that were used with
     * SMTP. They are no longer presented to the user as options, but they may
     * still exist in a user's settings from a previous version or may be found
     * when importing settings.
     */
    AUTOMATIC,
    LOGIN
}
