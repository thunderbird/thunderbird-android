package net.thunderbird.feature.notification.api.content

import net.thunderbird.core.common.io.KmpParcelize
import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.resources.Res
import net.thunderbird.feature.notification.resources.push_info_disable_push_action
import net.thunderbird.feature.notification.resources.push_notification_grant_alarm_permission
import net.thunderbird.feature.notification.resources.push_notification_info
import net.thunderbird.feature.notification.resources.push_notification_state_alarm_permission_missing
import net.thunderbird.feature.notification.resources.push_notification_state_initializing
import net.thunderbird.feature.notification.resources.push_notification_state_listening
import net.thunderbird.feature.notification.resources.push_notification_state_wait_background_sync
import net.thunderbird.feature.notification.resources.push_notification_state_wait_network
import org.jetbrains.compose.resources.getString

/**
 * Represents notifications related to the Push Notification Service.
 * Mostly used on Android.
 */
sealed class PushServiceNotification : AppNotification(), SystemNotification {
    override val severity: NotificationSeverity = NotificationSeverity.Information
    override val channel: NotificationChannel = NotificationChannel.PushService

    /**
     * This notification is shown when the Push Notification Foreground Service is initializing.
     * @property severity The severity level is set to [NotificationSeverity.Information].
     */
    @ConsistentCopyVisibility
    @KmpParcelize
    data class Initializing private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
    ) : PushServiceNotification() {
        companion object {
            /**
             * Creates an [Initializing] notification.
             *
             * @return An [Initializing] notification.
             */
            suspend operator fun invoke(): Initializing = Initializing(
                title = getString(resource = Res.string.push_notification_state_initializing),
                contentText = getString(resource = Res.string.push_notification_info),
                actions = buildNotificationActions(),
            )
        }
    }

    /**
     * This notification is displayed when the push service is actively listening for new messages.
     * @property severity The severity level is set to [NotificationSeverity.Information].
     */
    @ConsistentCopyVisibility
    @KmpParcelize
    data class Listening private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
    ) : PushServiceNotification() {
        companion object {
            /**
             * Creates a new [Listening] push service notification.
             *
             * @return A new [Listening] notification.
             */
            suspend operator fun invoke(): Listening = Listening(
                title = getString(resource = Res.string.push_notification_state_listening),
                contentText = getString(resource = Res.string.push_notification_info),
                actions = buildNotificationActions(),
            )
        }
    }

    /**
     * This notification is displayed when the app is waiting for background synchronization to complete.
     * @property severity The severity level is set to [NotificationSeverity.Information].
     */
    @ConsistentCopyVisibility
    @KmpParcelize
    data class WaitBackgroundSync private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
    ) : PushServiceNotification() {
        companion object {
            /**
             * Creates a [WaitBackgroundSync] notification.
             *
             * @return A [WaitBackgroundSync] notification.
             */
            suspend operator fun invoke(): WaitBackgroundSync = WaitBackgroundSync(
                title = getString(resource = Res.string.push_notification_state_wait_background_sync),
                contentText = getString(resource = Res.string.push_notification_info),
                actions = buildNotificationActions(),
            )
        }
    }

    /**
     * This notification is shown when the push service is waiting for a network connection.
     * @property severity The severity level is set to [NotificationSeverity.Information].
     */
    @ConsistentCopyVisibility
    @KmpParcelize
    data class WaitNetwork private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
    ) : PushServiceNotification() {
        companion object {
            /**
             * Creates a [WaitNetwork] notification.
             *
             * @return A [WaitNetwork] notification.
             */
            suspend operator fun invoke(): WaitNetwork = WaitNetwork(
                title = getString(resource = Res.string.push_notification_state_wait_network),
                contentText = getString(resource = Res.string.push_notification_info),
                actions = buildNotificationActions(),
            )
        }
    }

    /**
     * Represents a notification indicating that the alarm permission is missing.
     *
     * This notification is displayed when the app is missing the permission to schedule exact alarms,
     * which is necessary for the push service to function correctly.
     *
     * @property severity The severity level is set to [NotificationSeverity.Critical].
     */
    @ConsistentCopyVisibility
    @KmpParcelize
    data class AlarmPermissionMissing private constructor(
        override val title: String,
        override val contentText: String?,
    ) : PushServiceNotification(), InAppNotification {
        override val severity: NotificationSeverity = NotificationSeverity.Critical

        companion object {
            /**
             * Creates an [AlarmPermissionMissing] notification.
             *
             * @return An [AlarmPermissionMissing] instance.
             */
            suspend operator fun invoke(): AlarmPermissionMissing = AlarmPermissionMissing(
                title = getString(resource = Res.string.push_notification_state_alarm_permission_missing),
                contentText = getString(resource = Res.string.push_notification_grant_alarm_permission),
            )
        }
    }
}

/**
 * Builds a set of [NotificationAction] instances for the push service notification.
 *
 * This function is used to create the default actions that are displayed in the
 * push service notification.
 *
 * @return A set of [NotificationAction] instances.
 */
private suspend fun buildNotificationActions(): Set<NotificationAction> = setOf(
    NotificationAction.CustomAction(
        message = getString(resource = Res.string.push_info_disable_push_action),
    ),
)
