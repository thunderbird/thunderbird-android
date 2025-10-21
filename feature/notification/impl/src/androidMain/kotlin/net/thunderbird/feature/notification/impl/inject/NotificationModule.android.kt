package net.thunderbird.feature.notification.impl.inject

import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialogFragmentFactory
import net.thunderbird.feature.notification.impl.intent.action.AlarmPermissionMissingNotificationTapActionIntentCreator
import net.thunderbird.feature.notification.impl.intent.action.DefaultNotificationActionIntentCreator
import net.thunderbird.feature.notification.impl.intent.action.NotificationActionIntentCreator
import net.thunderbird.feature.notification.impl.intent.action.UpdateServerSettingsNotificationActionIntentCreator
import net.thunderbird.feature.notification.impl.receiver.AndroidSystemNotificationNotifier
import net.thunderbird.feature.notification.impl.receiver.SystemNotificationNotifier
import net.thunderbird.feature.notification.impl.ui.action.DefaultSystemNotificationActionCreator
import net.thunderbird.feature.notification.impl.ui.action.NotificationActionCreator
import net.thunderbird.feature.notification.impl.ui.dialog.ErrorNotificationsDialogFragment
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val platformFeatureNotificationModule: Module = module {
    single<List<NotificationActionIntentCreator<*, *>>>(named<NotificationActionIntentCreator.TypeQualifier>()) {
        listOf(
            AlarmPermissionMissingNotificationTapActionIntentCreator(
                context = androidApplication(),
                logger = get(),
            ),
            UpdateServerSettingsNotificationActionIntentCreator(
                context = androidApplication(),
                logger = get(),
            ),
            // The Default implementation must always be the last.
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

    single<NotificationNotifier<SystemNotification>>(named<SystemNotificationNotifier>()) {
        AndroidSystemNotificationNotifier(
            logger = get(),
            applicationContext = androidApplication(),
            notificationRegistry = get(),
            notificationActionCreator = get(named(NotificationActionCreator.TypeQualifier.System)),
        )
    }.onClose { notifier ->
        notifier?.dispose()
    }

    factory<ErrorNotificationsDialogFragmentFactory> { ErrorNotificationsDialogFragment.Factory }
}
