package net.thunderbird.feature.notification.impl.inject

import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.notification.impl.DefaultNotificationRegistry
import net.thunderbird.feature.notification.impl.command.NotificationCommandFactory
import net.thunderbird.feature.notification.impl.receiver.InAppNotificationNotifier
import net.thunderbird.feature.notification.impl.receiver.SystemNotificationNotifier
import net.thunderbird.feature.notification.impl.sender.DefaultNotificationSender
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal expect val platformFeatureNotificationModule: Module

val featureNotificationModule = module {
    includes(platformFeatureNotificationModule)

    factory<NotificationNotifier<InAppNotification>>(named<InAppNotificationNotifier>()) {
        InAppNotificationNotifier()
    }

    factory<NotificationCommandFactory> {
        NotificationCommandFactory(
            logger = get(),
            featureFlagProvider = get(),
            notificationRegistry = get(),
            systemNotificationNotifier = get(named<SystemNotificationNotifier>()),
            inAppNotificationNotifier = get(named<InAppNotificationNotifier>()),
        )
    }

    single<NotificationSender> {
        DefaultNotificationSender(
            commandFactory = get(),
        )
    }

    single<NotificationRegistry> { DefaultNotificationRegistry() }
}
