package net.thunderbird.feature.notification.impl.receiver

import net.thunderbird.core.common.provider.ContextProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * This notifier is responsible for taking a [InAppNotification] data object and
 * presenting it to the user in a suitable way.
 */
internal interface InAppNotificationNotifier : NotificationNotifier<InAppNotification>

internal expect inline fun <reified TContext> InAppNotificationNotifier(
    logger: Logger,
    contextProvider: ContextProvider<TContext>,
): InAppNotificationNotifier
