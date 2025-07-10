package net.thunderbird.feature.notification.impl.receiver

import net.thunderbird.core.common.provider.ContextProvider
import net.thunderbird.core.logging.Logger

internal actual inline fun <reified TContext> SystemNotificationNotifier(
    logger: Logger,
    contextProvider: ContextProvider<TContext>,
): SystemNotificationNotifier {
    error("Can't send system notification from a jvm library. Use android library or app instead.")
}
