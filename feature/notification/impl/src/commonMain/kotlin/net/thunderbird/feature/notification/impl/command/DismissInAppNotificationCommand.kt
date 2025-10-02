package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

private const val TAG = "DismissInAppNotificationCommand"

class DismissInAppNotificationCommand(
    logger: Logger,
    featureFlagProvider: FeatureFlagProvider,
    notificationRegistry: NotificationRegistry,
    notification: InAppNotification,
    notifier: NotificationNotifier<InAppNotification>,
) : DismissNotificationCommand<InAppNotification>(
    logTag = TAG,
    logger = logger,
    featureFlagProvider = featureFlagProvider,
    notificationRegistry = notificationRegistry,
    notification = notification,
    notifier = notifier,
) {
    override val featureFlagKey: FeatureFlagKey = FeatureFlagKey.DisplayInAppNotifications
}
