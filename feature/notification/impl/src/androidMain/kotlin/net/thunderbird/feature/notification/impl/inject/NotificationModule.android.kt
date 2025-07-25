package net.thunderbird.feature.notification.impl.inject

import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.impl.intent.action.DefaultNotificationActionIntentCreator
import net.thunderbird.feature.notification.impl.intent.action.NotificationActionIntentCreator
import net.thunderbird.feature.notification.impl.ui.action.DefaultSystemNotificationActionCreator
import net.thunderbird.feature.notification.impl.ui.action.NotificationActionCreator
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual val platformFeatureNotificationModule: Module = module {
    single<List<NotificationActionIntentCreator<*>>>(named<NotificationActionIntentCreator.TypeQualifier>()) {
        listOf(
            DefaultNotificationActionIntentCreator(
                logger = get(),
                applicationContext = androidApplication(),
            ),
        )
    }

    single<NotificationActionCreator<SystemNotification>>(named(NotificationActionCreator.TypeQualifier.System)) {
        DefaultSystemNotificationActionCreator(
            logger = get(),
            actionIntentCreators = get(named<NotificationActionIntentCreator.TypeQualifier>()),
        )
    }
}
