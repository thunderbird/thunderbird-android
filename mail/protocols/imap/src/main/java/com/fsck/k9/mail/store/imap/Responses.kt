package com.fsck.k9.mail.store.imap

internal object Responses {
    const val CAPABILITY: String = "CAPABILITY"
    const val NAMESPACE: String = "NAMESPACE"
    const val LIST: String = "LIST"
    const val LSUB: String = "LSUB"
    const val OK: String = "OK"
    const val NO: String = "NO"
    const val BAD: String = "BAD"
    const val PREAUTH: String = "PREAUTH"
    const val BYE: String = "BYE"
    const val EXISTS: String = "EXISTS"
    const val EXPUNGE: String = "EXPUNGE"
    const val PERMANENTFLAGS: String = "PERMANENTFLAGS"
    const val COPYUID: String = "COPYUID"
    const val SEARCH: String = "SEARCH"
    const val UIDVALIDITY: String = "UIDVALIDITY"
    const val ENABLED: String = "ENABLED"
}
