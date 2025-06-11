package net.thunderbird.feature.notification.impl.receiver

import net.thunderbird.core.common.provider.ContextProvider
import net.thunderbird.core.logging.Logger

internal actual inline fun <reified TContext> InAppNotificationNotifier(
    logger: Logger,
    contextProvider: ContextProvider<TContext>,
): InAppNotificationNotifier {
    error("Can't send in-app notification from a jvm library. Use android library or app instead.")
}
