package com.fsck.k9.mail.store.webdav;

/**
 * WebDAV constants
 */
class WebDavConstants {

    // Authentication types
    static final short AUTH_TYPE_NONE = 0;
    static final short AUTH_TYPE_BASIC = 1;
    static final short AUTH_TYPE_FORM_BASED = 2;

    static final String[] EMPTY_STRING_ARRAY = new String[0];

    // These are the ids used from Exchange server to identify the special folders
    // http://social.technet.microsoft.com/Forums/en/exchangesvrdevelopment/thread/1cd2e98c-8a12-44bd-a3e3-9c5ee9e4e14d
    static final String DAV_MAIL_INBOX_FOLDER = "inbox";
    static final String DAV_MAIL_DRAFTS_FOLDER = "drafts";
    static final String DAV_MAIL_SPAM_FOLDER = "junkemail";
    static final String DAV_MAIL_SEND_FOLDER = "##DavMailSubmissionURI##";
    static final String DAV_MAIL_TRASH_FOLDER = "deleteditems";
    static final String DAV_MAIL_OUTBOX_FOLDER = "outbox";
    static final String DAV_MAIL_SENT_FOLDER = "sentitems";
}
