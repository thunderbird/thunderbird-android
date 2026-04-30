package net.thunderbird.core.preference.notification

import net.thunderbird.core.preference.PreferenceManager

enum class NotificationSettingKey(val value: String) {

    QuietTimeEnds("quietTimeEnds"),
    QuietTimeStarts("quietTimeStarts"),
    QuietTimeEnabled("quietTimeEnabled"),
    NotificationDuringQuietTimeEnabled("notificationDuringQuietTimeEnabled"),
    MessageActionsOrder("messageActionsOrder"),
    MessageActionsCutoff("messageActionsCutoff"),
    IsSummaryDeleteActionEnabled("isSummaryDeleteActionEnabled"),
    NotificationQuickDeleteBehaviour("notificationQuickDelete"),
    LockScreenNotificationVisibility("lockScreenNotificationVisibility"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}
interface NotificationPreferenceManager : PreferenceManager<NotificationPreference>
