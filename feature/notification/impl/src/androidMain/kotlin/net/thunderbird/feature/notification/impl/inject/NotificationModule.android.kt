package net.thunderbird.feature.notification.impl.inject

import android.content.Context
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.impl.intent.AlarmPermissionMissingNotificationIntentCreator
import net.thunderbird.feature.notification.impl.intent.SystemNotificationIntentCreator
import net.thunderbird.feature.notification.impl.intent.action.DefaultNotificationActionIntentCreator
import net.thunderbird.feature.notification.impl.intent.action.NotificationActionIntentCreator
import net.thunderbird.feature.notification.impl.receiver.InAppNotificationNotifier
import net.thunderbird.feature.notification.impl.receiver.SystemNotificationNotifier
import net.thunderbird.feature.notification.impl.ui.action.DefaultSystemNotificationActionCreator
import net.thunderbird.feature.notification.impl.ui.action.NotificationActionCreator
import org.koin.android.ext.koin.androidApplication
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

    factory<NotificationNotifier<SystemNotification>>(
        qualifier = named<NotificationNotifier.TypeQualifier.SystemNotification>(),
    ) {
        SystemNotificationNotifier<Context>(
            logger = get(),
            contextProvider = get(),
        )
    }.onClose { notifier ->
        notifier?.dispose()
    }

    single<List<SystemNotificationIntentCreator<*>>>(named<SystemNotificationIntentCreator.TypeQualifier>()) {
        listOf(
            AlarmPermissionMissingNotificationIntentCreator(
                context = androidApplication(),
                logger = get(),
            ),
        )
    }

    single<List<NotificationActionIntentCreator<*>>>(named<NotificationActionIntentCreator.TypeQualifier>()) {
        listOf(
            DefaultNotificationActionIntentCreator(
                logger = get(),
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
