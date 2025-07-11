package com.fsck.k9.mail

import net.thunderbird.core.common.exception.MessagingException

@Deprecated(
    message = "Use net.thunderbird.core.common.exception.MessagingException instead",
    replaceWith = ReplaceWith(
        expression = "MessagingException",
        "net.thunderbird.core.common.exception.MessagingException",
    ),
)
open class MessagingException : MessagingException {
    var isPermanentFailure: Boolean = false
        private set

    constructor(cause: Throwable?) : super(cause)

    constructor(message: String?) : super(message)

    constructor(message: String?, perm: Boolean) : super(message) {
        this.isPermanentFailure = perm
    }

    constructor(message: String?, throwable: Throwable?) : super(message, throwable)

    constructor(message: String?, perm: Boolean, throwable: Throwable?) : super(message, throwable) {
        this.isPermanentFailure = perm
    }

    companion object {
        val serialVersionUID: Long = -1
    }
}
