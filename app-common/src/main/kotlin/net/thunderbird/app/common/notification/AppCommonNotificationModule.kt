package net.thunderbird.app.common.notification

import net.thunderbird.feature.notification.api.NotificationIdFactory
import org.koin.dsl.module

internal val appCommonNotificationModule = module {
    single<NotificationIdFactory> { LegacyNotificationIdFactory() }
}
