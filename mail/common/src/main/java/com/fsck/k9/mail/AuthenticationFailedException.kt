package com.fsck.k9.mail

import net.thunderbird.core.common.exception.MessagingException

class AuthenticationFailedException @JvmOverloads constructor(
    message: String,
    throwable: Throwable? = null,
    val messageFromServer: String? = null,
) : MessagingException(message, throwable) {
    val isMessageFromServerAvailable = messageFromServer != null
}
