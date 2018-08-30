package com.fsck.k9.mail

class AuthenticationFailedException @JvmOverloads constructor(
        message: String,
        throwable: Throwable? = null,
        messageFromServer: String? = null
) : MessagingException(message, throwable) {
}
