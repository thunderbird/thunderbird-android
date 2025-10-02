package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

private const val TAG = "DismissSystemNotificationCommand"

class DismissSystemNotificationCommand(
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    private val notificationRegistry: NotificationRegistry,
    notification: SystemNotification,
    notifier: NotificationNotifier<SystemNotification>,
) : DismissNotificationCommand<SystemNotification>(
    logTag = TAG,
    logger = logger,
    featureFlagProvider = featureFlagProvider,
    notificationRegistry = notificationRegistry,
    notification = notification,
    notifier = notifier,
) {
    override val featureFlagKey: FeatureFlagKey = FeatureFlagKey.UseNotificationSenderForSystemNotifications
}
