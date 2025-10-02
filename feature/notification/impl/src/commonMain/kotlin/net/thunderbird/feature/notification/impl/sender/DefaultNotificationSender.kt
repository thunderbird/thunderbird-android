package net.thunderbird.feature.notification.impl.sender

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.outcome.CommandNotCreated
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.notification.impl.command.DisplayInAppNotificationCommand
import net.thunderbird.feature.notification.impl.command.DisplaySystemNotificationCommand

/**
 * Responsible for sending notifications by creating and executing the appropriate commands.
 *
 * This class determines the type of the incoming [Notification] and constructs
 * the relevant [NotificationCommand]s (e.g., [DisplaySystemNotificationCommand] for [SystemNotification],
 * [DisplayInAppNotificationCommand] for [InAppNotification]). It then executes each command
 * and emits the result of the execution as a [Flow].
 *
 * @param logger The logger instance for logging events.
 * @param featureFlagProvider Provider for accessing feature flag states.
 * @param notificationRegistry Registry for managing notifications.
 * @param systemNotificationNotifier Notifier specifically for system notifications.
 * @param inAppNotificationNotifier Notifier specifically for in-app notifications.
 */
class DefaultNotificationSender internal constructor(
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    private val notificationRegistry: NotificationRegistry,
    private val systemNotificationNotifier: NotificationNotifier<SystemNotification>,
    private val inAppNotificationNotifier: NotificationNotifier<InAppNotification>,
) : NotificationSender {
    override fun send(notification: Notification): Flow<NotificationCommandOutcome<Notification>> = flow {
        val commands = buildCommands(notification)
        commands
            .ifEmpty {
                val message = "No commands to execute for notification $notification"
                logger.warn { message }
                emit(Outcome.failure(CommandNotCreated(message)))
                emptyList()
            }
            .forEach { command -> emit(command.execute()) }
    }

    private fun buildCommands(notification: Notification): List<NotificationCommand<out Notification>> = buildList {
        if (notification is SystemNotification) {
            add(
                DisplaySystemNotificationCommand(
                    logger = logger,
                    featureFlagProvider = featureFlagProvider,
                    notificationRegistry = notificationRegistry,
                    notification = notification,
                    notifier = systemNotificationNotifier,
                ),
            )
        }

        if (notification is InAppNotification) {
            add(
                DisplayInAppNotificationCommand(
                    logger = logger,
                    featureFlagProvider = featureFlagProvider,
                    notificationRegistry = notificationRegistry,
                    notification = notification,
                    notifier = inAppNotificationNotifier,
                ),
            )
        }
    }
}
