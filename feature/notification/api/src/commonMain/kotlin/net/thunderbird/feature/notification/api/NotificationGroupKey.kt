package net.thunderbird.feature.notification.api

/**
 * Represents a key for a notification group.
 *
 * This class is used to uniquely identify a group of notifications that should be displayed together.
 * For example, all notifications related to a specific account could be grouped together using a
 * NotificationGroupKey.
 *
 * @param value The string value of the notification group key.
 */
@JvmInline
value class NotificationGroupKey(val value: String)
