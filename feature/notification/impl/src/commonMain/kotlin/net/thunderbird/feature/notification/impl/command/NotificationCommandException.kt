package net.thunderbird.feature.notification.impl.command

class NotificationCommandException @JvmOverloads constructor(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message, cause)
