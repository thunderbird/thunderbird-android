package net.thunderbird.core.preference.notification

import net.thunderbird.core.preference.PreferenceManager

const val KEY_QUIET_TIME_ENDS = "quietTimeEnds"
const val KEY_QUIET_TIME_STARTS = "quietTimeStarts"
const val KEY_QUIET_TIME_ENABLED = "quietTimeEnabled"
const val KEY_NOTIFICATION_DURING_QUIET_TIME_ENABLED = "notificationDuringQuietTimeEnabled"
const val KEY_MESSAGE_ACTIONS_ORDER = "messageActionsOrder"
const val KEY_MESSAGE_ACTIONS_CUTOFF = "messageActionsCutoff"
const val KEY_IS_SUMMARY_DELETE_ACTION_ENABLED = "isSummaryDeleteActionEnabled"
const val KEY_SHOW_CONTACT_PICTURE_IN_NOTIFICATION = "showContactPictureInNotification"

const val KEY_NOTIFICATION_QUICK_DELETE_BEHAVIOUR = "notificationQuickDelete"
const val KEY_LOCK_SCREEN_NOTIFICATION_VISIBILITY = "lockScreenNotificationVisibility"

interface NotificationPreferenceManager : PreferenceManager<NotificationPreference>
