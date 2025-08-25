package com.fsck.k9.mail.store.imap

internal object Capabilities {
    const val IDLE: String = "IDLE"
    const val CONDSTORE: String = "CONDSTORE"
    const val SASL_IR: String = "SASL-IR"
    const val AUTH_XOAUTH2: String = "AUTH=XOAUTH2"
    const val AUTH_OAUTHBEARER: String = "AUTH=OAUTHBEARER"
    const val AUTH_CRAM_MD5: String = "AUTH=CRAM-MD5"
    const val AUTH_PLAIN: String = "AUTH=PLAIN"
    const val AUTH_EXTERNAL: String = "AUTH=EXTERNAL"
    const val LOGINDISABLED: String = "LOGINDISABLED"
    const val NAMESPACE: String = "NAMESPACE"
    const val COMPRESS_DEFLATE: String = "COMPRESS=DEFLATE"
    const val ID: String = "ID"
    const val STARTTLS: String = "STARTTLS"
    const val SPECIAL_USE: String = "SPECIAL-USE"
    const val UID_PLUS: String = "UIDPLUS"
    const val LIST_EXTENDED: String = "LIST-EXTENDED"
    const val MOVE: String = "MOVE"
    const val ENABLE: String = "ENABLE"
    const val CREATE_SPECIAL_USE: String = "CREATE-SPECIAL-USE"
    const val UTF8_ACCEPT: String = "UTF8=ACCEPT"
}
