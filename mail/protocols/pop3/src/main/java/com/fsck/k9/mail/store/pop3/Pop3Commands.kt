package com.fsck.k9.mail.store.pop3

internal object Pop3Commands {
    const val STLS_COMMAND = "STLS"
    const val USER_COMMAND = "USER"
    const val PASS_COMMAND = "PASS"
    const val CAPA_COMMAND = "CAPA"
    const val AUTH_COMMAND = "AUTH"
    const val STAT_COMMAND = "STAT"
    const val LIST_COMMAND = "LIST"
    const val UIDL_COMMAND = "UIDL"
    const val TOP_COMMAND = "TOP"
    const val RETR_COMMAND = "RETR"
    const val DELE_COMMAND = "DELE"
    const val QUIT_COMMAND = "QUIT"

    const val STLS_CAPABILITY = "STLS"
    const val UIDL_CAPABILITY = "UIDL"
    const val TOP_CAPABILITY = "TOP"
    const val SASL_CAPABILITY = "SASL"
    const val AUTH_PLAIN_CAPABILITY = "PLAIN"
    const val AUTH_CRAM_MD5_CAPABILITY = "CRAM-MD5"
    const val AUTH_EXTERNAL_CAPABILITY = "EXTERNAL"
}
