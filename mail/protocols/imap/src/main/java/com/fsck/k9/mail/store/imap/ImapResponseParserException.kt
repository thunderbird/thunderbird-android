package com.fsck.k9.mail.store.imap

class ImapResponseParserException @JvmOverloads constructor(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
