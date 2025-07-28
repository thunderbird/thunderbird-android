package net.thunderbird.feature.notification.api.command

/**
 * Represents an exception that occurs during the execution of a notification command.
 *
 * This exception is typically used when there is an issue with processing a notification command,
 * such as invalid parameters, inability to reach the notification service, or other errors
 * specific to the notification process.
 *
 * @property message A detailed message explaining the reason for the exception.
 * @property cause The underlying cause of the exception, if any.
 */
class NotificationCommandException @JvmOverloads constructor(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message, cause)
