@file:JvmName("ThrowableExtensions")

package net.thunderbird.core.common.exception

val Throwable.rootCauseMessage: String?
    get() {
        var rootCause = this
        var nextCause: Throwable? = null
        do {
            nextCause = rootCause.cause?.also {
                rootCause = it
            }
        } while (nextCause != null)

        if (rootCause is MessagingException) {
            return rootCause.message
        }

        // Remove the namespace on the exception so we have a fighting chance of seeing more
        // of the error in the notification.
        val simpleName = rootCause::class.simpleName
        val message = rootCause.localizedMessage
        return if (message.isNullOrBlank()) {
            simpleName
        } else {
            "$simpleName: $message"
        }
    }
