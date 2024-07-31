package app.k9mail.legacy.notification

/**
 * Describes how a notification should behave.
 */
data class NotificationSettings(
    val isRingEnabled: Boolean = false,
    val ringtone: String? = null,
    val light: NotificationLight = NotificationLight.Disabled,
    val vibration: NotificationVibration = NotificationVibration.DEFAULT,
)
