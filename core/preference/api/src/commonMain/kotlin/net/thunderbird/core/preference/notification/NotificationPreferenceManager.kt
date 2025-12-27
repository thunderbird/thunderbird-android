package net.thunderbird.core.preference.notification

import net.thunderbird.core.preference.PreferenceManager

const val KEY_QUIET_TIME_ENDS = "quietTimeEnds"
const val KEY_QUIET_TIME_STARTS = "quietTimeStarts"
const val KEY_QUIET_TIME_ENABLED = "quietTimeEnabled"
const val KEY_NOTIFICATION_DURING_QUIET_TIME_ENABLED = "notificationDuringQuietTimeEnabled"
const val KEY_MESSAGE_ACTIONS_ORDER = "messageActionsOrder"
const val KEY_MESSAGE_ACTIONS_CUTOFF = "messageActionsCutoff"
const val KEY_IS_SUMMARY_DELETE_ACTION_ENABLED = "isSummaryDeleteActionEnabled"

interface NotificationPreferenceManager : PreferenceManager<NotificationPreference>
