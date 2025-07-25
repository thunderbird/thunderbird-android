package net.thunderbird.feature.notification.impl.inject

import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.notification.impl.command.NotificationCommandFactory
import net.thunderbird.feature.notification.impl.receiver.InAppNotificationNotifier
import net.thunderbird.feature.notification.impl.receiver.SystemNotificationNotifier
import net.thunderbird.feature.notification.impl.sender.DefaultNotificationSender
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect val platformFeatureNotificationModule: Module

val featureNotificationModule = module {
    includes(platformFeatureNotificationModule)

    factory { SystemNotificationNotifier() }
    factory { InAppNotificationNotifier() }

    factory<NotificationCommandFactory> {
        NotificationCommandFactory(
            logger = get(),
            systemNotificationNotifier = get(),
            inAppNotificationNotifier = get(),
        )
    }

    single<NotificationSender> {
        DefaultNotificationSender(
            commandFactory = get(),
        )
    }
}
