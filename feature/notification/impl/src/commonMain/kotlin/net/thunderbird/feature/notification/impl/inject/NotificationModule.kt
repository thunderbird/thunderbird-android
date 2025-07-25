package net.thunderbird.feature.notification.impl.inject

import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.notification.impl.DefaultNotificationRegistry
import net.thunderbird.feature.notification.impl.command.NotificationCommandFactory
import net.thunderbird.feature.notification.impl.receiver.InAppNotificationNotifier
import net.thunderbird.feature.notification.impl.receiver.SystemNotificationNotifier
import net.thunderbird.feature.notification.impl.sender.DefaultNotificationSender
import org.koin.dsl.module

val featureNotificationModule = module {
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

    single<NotificationRegistry> { DefaultNotificationRegistry() }
}
