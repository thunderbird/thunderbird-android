package net.thunderbird.core.preference.notification

import net.thunderbird.core.preference.PreferenceManager

const val KEY_QUIET_TIME_ENDS = "quietTimeEnds"
const val KEY_QUIET_TIME_STARTS = "quietTimeStarts"
const val KEY_QUIET_TIME_ENABLED = "quietTimeEnabled"
const val KEY_NOTIFICATION_DURING_QUIET_TIME_ENABLED = "notificationDuringQuietTimeEnabled"

interface NotificationPreferenceManager : PreferenceManager<NotificationPreference>
