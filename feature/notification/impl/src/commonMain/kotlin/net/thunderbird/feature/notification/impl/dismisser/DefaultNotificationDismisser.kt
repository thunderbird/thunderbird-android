package net.thunderbird.feature.notification.impl.dismisser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.dismisser.NotificationDismisser
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.impl.command.DismissInAppNotificationCommand
import net.thunderbird.feature.notification.impl.command.DismissSystemNotificationCommand

private const val TAG = "DefaultNotificationDismisser"

/**
 * Responsible for dismissing notifications by creating and executing the appropriate commands.
 *
 * This class determines the type of the incoming [Notification] and constructs
 * the relevant [NotificationCommand]s (e.g., [DismissSystemNotificationCommand] for [SystemNotification],
 * [DismissInAppNotificationCommand] for [InAppNotification]). It then executes each command
 * and emits the result of the execution as a [Flow].
 *
 * @param logger The logger instance for logging events.
 * @param featureFlagProvider Provider for accessing feature flag states.
 * @param notificationRegistry Registry for managing notifications.
 * @param systemNotificationNotifier Notifier specifically for system notifications.
 * @param inAppNotificationNotifier Notifier specifically for in-app notifications.
 */
class DefaultNotificationDismisser internal constructor(
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    private val notificationRegistry: NotificationRegistry,
    private val systemNotificationNotifier: NotificationNotifier<SystemNotification>,
    private val inAppNotificationNotifier: NotificationNotifier<InAppNotification>,
) : NotificationDismisser {
    override fun dismiss(id: NotificationId): Flow<Outcome<Success<Notification>, Failure<Notification>>> = flow {
        logger.verbose(TAG) { "dismiss() called with: id = $id" }
        val notification = notificationRegistry[id]
        if (notification == null) {
            emit(
                value = Outcome.failure(
                    error = Failure(
                        command = null,
                        throwable = NotificationCommandException(message = "Notification with id '$id' not found"),
                    ),
                ),
            )
        } else {
            emitAll(dismiss(notification))
        }
    }

    override fun dismiss(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>> =
        flow {
            logger.verbose(TAG) { "dismiss() called with: notification = $notification" }

            if (notification in notificationRegistry) {
                val commands = buildCommands(notification)
                commands.forEach { command -> emit(command.execute()) }
            } else {
                emit(
                    value = Outcome.failure(
                        error = Failure(
                            command = null,
                            throwable = NotificationCommandException(
                                message = "Can't dismiss notification that is already dismissed",
                            ),
                        ),
                    ),
                )
            }
        }

    private fun buildCommands(notification: Notification): List<NotificationCommand<out Notification>> = buildList {
        if (notification is SystemNotification) {
            add(
                DismissSystemNotificationCommand(
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
                DismissInAppNotificationCommand(
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
