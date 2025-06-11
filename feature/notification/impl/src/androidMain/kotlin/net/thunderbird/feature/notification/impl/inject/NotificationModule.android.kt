package net.thunderbird.feature.notification.impl.inject

import android.content.Context
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.impl.receiver.InAppNotificationNotifier
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val platformFeatureNotificationModule: Module = module {
    single<NotificationNotifier<InAppNotification>>(
        qualifier = named<NotificationNotifier.TypeQualifier.InAppNotification>(),
    ) {
        InAppNotificationNotifier<Context>(
            logger = get(),
            contextProvider = get(),
        )
    }.onClose { notifier ->
        notifier?.dispose()
    }
}
