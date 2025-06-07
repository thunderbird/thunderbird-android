package com.fsck.k9.mail.store.imap

class ImapResponseParserException : RuntimeException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
