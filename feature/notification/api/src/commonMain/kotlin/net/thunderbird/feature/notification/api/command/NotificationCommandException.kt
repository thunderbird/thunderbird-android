package net.thunderbird.feature.notification.api.command

class NotificationCommandException @JvmOverloads constructor(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message, cause)
