package net.thunderbird.feature.notification.impl.inject

import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.notification.impl.command.NotificationCommandFactory
import net.thunderbird.feature.notification.impl.receiver.SystemNotificationNotifier
import net.thunderbird.feature.notification.impl.sender.DefaultNotificationSender
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal expect val platformFeatureNotificationModule: Module

val featureNotificationModule = module {
    includes(platformFeatureNotificationModule)
    factory<NotificationNotifier<SystemNotification>>(
        qualifier = named<NotificationNotifier.TypeQualifier.SystemNotification>(),
    ) { SystemNotificationNotifier() }
    factory<NotificationCommandFactory> {
        NotificationCommandFactory(
            systemNotificationNotifier = get(named<NotificationNotifier.TypeQualifier.SystemNotification>()),
            inAppNotificationNotifier = get(named<NotificationNotifier.TypeQualifier.InAppNotification>()),
        )
    }

    single<NotificationSender> {
        DefaultNotificationSender(
            commandFactory = get(),
        )
    }
}
