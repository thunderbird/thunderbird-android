package net.thunderbird.core.preference.notification

import net.thunderbird.core.preference.NotificationQuickDelete

const val NOTIFICATION_PREFERENCE_DEFAULT_IS_QUIET_TIME_ENABLED = false
const val NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_STARTS = "21:00"
const val NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_END = "7:00"
const val NOTIFICATION_PREFERENCE_DEFAULT_IS_NOTIFICATION_DURING_QUIET_TIME_ENABLED = true
val NOTIFICATION_PREFERENCE_DEFAULT_QUICK_DELETE_BEHAVIOUR = NotificationQuickDelete.ALWAYS

data class NotificationPreference(
    val isQuietTimeEnabled: Boolean = NOTIFICATION_PREFERENCE_DEFAULT_IS_QUIET_TIME_ENABLED,
    val quietTimeStarts: String = NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_STARTS,
    val quietTimeEnds: String = NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_END,
    val isNotificationDuringQuietTimeEnabled: Boolean =
        NOTIFICATION_PREFERENCE_DEFAULT_IS_NOTIFICATION_DURING_QUIET_TIME_ENABLED,
    val notificationQuickDeleteBehaviour: NotificationQuickDelete =
        NOTIFICATION_PREFERENCE_DEFAULT_QUICK_DELETE_BEHAVIOUR,
)
