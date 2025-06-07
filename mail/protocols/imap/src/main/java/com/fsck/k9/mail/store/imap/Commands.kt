package com.fsck.k9.mail.store.imap

internal object Commands {
    const val IDLE: String = "IDLE"
    const val NAMESPACE: String = "NAMESPACE"
    const val CAPABILITY: String = "CAPABILITY"
    const val COMPRESS_DEFLATE: String = "COMPRESS DEFLATE"
    const val STARTTLS: String = "STARTTLS"
    const val AUTHENTICATE_XOAUTH2: String = "AUTHENTICATE XOAUTH2"
    const val AUTHENTICATE_OAUTHBEARER: String = "AUTHENTICATE OAUTHBEARER"
    const val AUTHENTICATE_CRAM_MD5: String = "AUTHENTICATE CRAM-MD5"
    const val AUTHENTICATE_PLAIN: String = "AUTHENTICATE PLAIN"
    const val AUTHENTICATE_EXTERNAL: String = "AUTHENTICATE EXTERNAL"
    const val LOGIN: String = "LOGIN"
    const val LIST: String = "LIST"
    const val NOOP: String = "NOOP"
    const val UID_SEARCH: String = "UID SEARCH"
    const val UID_STORE: String = "UID STORE"
    const val UID_FETCH: String = "UID FETCH"
    const val UID_COPY: String = "UID COPY"
    const val UID_MOVE: String = "UID MOVE"
    const val UID_EXPUNGE: String = "UID EXPUNGE"
    const val ENABLE: String = "ENABLE UTF8=ACCEPT"
}
