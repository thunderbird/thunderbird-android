package net.thunderbird.feature.notification.api.content

import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.action.icon.DisablePushAction
import net.thunderbird.feature.notification.api.ui.action.icon.NotificationActionIcons
import net.thunderbird.feature.notification.api.ui.icon.AlarmPermissionMissing
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons
import net.thunderbird.feature.notification.api.ui.icon.PushServiceInitializing
import net.thunderbird.feature.notification.api.ui.icon.PushServiceListening
import net.thunderbird.feature.notification.api.ui.icon.PushServiceWaitBackgroundSync
import net.thunderbird.feature.notification.api.ui.icon.PushServiceWaitNetwork
import net.thunderbird.feature.notification.resources.api.Res
import net.thunderbird.feature.notification.resources.api.push_info_disable_push_action
import net.thunderbird.feature.notification.resources.api.push_notification_grant_alarm_permission
import net.thunderbird.feature.notification.resources.api.push_notification_info
import net.thunderbird.feature.notification.resources.api.push_notification_state_alarm_permission_missing
import net.thunderbird.feature.notification.resources.api.push_notification_state_initializing
import net.thunderbird.feature.notification.resources.api.push_notification_state_listening
import net.thunderbird.feature.notification.resources.api.push_notification_state_wait_background_sync
import net.thunderbird.feature.notification.resources.api.push_notification_state_wait_network
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
    data class Initializing private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
        override val icon: NotificationIcon = NotificationIcons.PushServiceInitializing,
    ) : PushServiceNotification() {
        override val accountUuid: String? = null

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
    data class Listening private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
        override val icon: NotificationIcon = NotificationIcons.PushServiceListening,
    ) : PushServiceNotification() {
        override val accountUuid: String? = null

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
    data class WaitBackgroundSync private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
        override val icon: NotificationIcon = NotificationIcons.PushServiceWaitBackgroundSync,
    ) : PushServiceNotification() {
        override val accountUuid: String? = null

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
    data class WaitNetwork private constructor(
        override val title: String,
        override val contentText: String?,
        override val actions: Set<NotificationAction>,
        override val icon: NotificationIcon = NotificationIcons.PushServiceWaitNetwork,
    ) : PushServiceNotification() {
        override val accountUuid: String? = null

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
    data class AlarmPermissionMissing private constructor(
        override val title: String,
        override val contentText: String?,
        override val icon: NotificationIcon = NotificationIcons.AlarmPermissionMissing,
    ) : PushServiceNotification(), InAppNotification {
        override val accountUuid: String? = null
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
    NotificationAction.Tap(),
    NotificationAction.CustomAction(
        title = getString(resource = Res.string.push_info_disable_push_action),
        icon = NotificationActionIcons.DisablePushAction,
    ),
)
