package net.thunderbird.feature.notification.impl.inject

import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.dismisser.NotificationDismisser
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.api.receiver.SystemNotificationStream
import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.notification.impl.DefaultNotificationManager
import net.thunderbird.feature.notification.impl.DefaultNotificationRegistry
import net.thunderbird.feature.notification.impl.dismisser.DefaultNotificationDismisser
import net.thunderbird.feature.notification.impl.receiver.DefaultInAppNotificationStream
import net.thunderbird.feature.notification.impl.receiver.DefaultSystemNotificationStream
import net.thunderbird.feature.notification.impl.receiver.InAppNotificationNotifier
import net.thunderbird.feature.notification.impl.receiver.SystemNotificationNotifier
import net.thunderbird.feature.notification.impl.sender.DefaultNotificationSender
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

internal expect val platformFeatureNotificationModule: Module

val featureNotificationModule = module {
    includes(platformFeatureNotificationModule)

    single<DefaultNotificationRegistry> { DefaultNotificationRegistry() } binds arrayOf(NotificationRegistry::class)

    single<NotificationNotifier<InAppNotification>>(named<InAppNotificationNotifier>()) {
        InAppNotificationNotifier(
            logger = get(),
            notificationRegistry = get(),
        )
    }

    single<NotificationSender> {
        DefaultNotificationSender(
            logger = get(),
            featureFlagProvider = get(),
            systemNotificationNotifier = get(named<SystemNotificationNotifier>()),
            inAppNotificationNotifier = get(named<InAppNotificationNotifier>()),
        )
    }

    single<NotificationDismisser> {
        DefaultNotificationDismisser(
            logger = get(),
            featureFlagProvider = get(),
            notificationRegistry = get(),
            systemNotificationNotifier = get(named<SystemNotificationNotifier>()),
            inAppNotificationNotifier = get(named<InAppNotificationNotifier>()),
        )
    }

    single<NotificationManager> {
        DefaultNotificationManager(
            notificationSender = get(),
            notificationDismisser = get(),
        )
    }

    single<InAppNotificationStream> { DefaultInAppNotificationStream(registry = get()) }
    single<SystemNotificationStream> { DefaultSystemNotificationStream(registry = get()) }
}
