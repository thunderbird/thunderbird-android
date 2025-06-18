package net.thunderbird.feature.notification.impl.receiver

import net.thunderbird.core.common.provider.ContextProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * This notifier is responsible for taking a [SystemNotification] data object and
 * presenting it to the user in a suitable way.
 */
internal interface SystemNotificationNotifier : NotificationNotifier<SystemNotification>

internal expect inline fun <reified TContext> SystemNotificationNotifier(
    logger: Logger,
    contextProvider: ContextProvider<TContext>,
): SystemNotificationNotifier
