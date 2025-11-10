package net.thunderbird.feature.notification.api.ui.icon

import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined.Warning
import net.thunderbird.feature.notification.api.R
import net.thunderbird.feature.notification.api.ui.icon.atom.Notification

private val Warning = Icons.Outlined.Warning
internal actual val NotificationIcons.AuthenticationError: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_warning,
        inAppNotificationIcon = Warning,
    )

internal actual val NotificationIcons.CertificateError: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_warning,
        inAppNotificationIcon = Warning,
    )

internal actual val NotificationIcons.FailedToCreate: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_warning,
        inAppNotificationIcon = Warning,
    )

internal actual val NotificationIcons.MailFetching: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_sync_animated,
    )

internal actual val NotificationIcons.MailSending: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_sync_animated,
    )

internal actual val NotificationIcons.MailSendFailed: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_warning,
        inAppNotificationIcon = Warning,
    )

internal actual val NotificationIcons.NewMailSingleMail: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_new_email,
    )

internal actual val NotificationIcons.NewMailSummaryMail: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_new_email,
    )

internal actual val NotificationIcons.PushServiceInitializing: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_notification,
    )

internal actual val NotificationIcons.PushServiceListening: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_notification,
    )

internal actual val NotificationIcons.PushServiceWaitBackgroundSync: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_notification,
    )

internal actual val NotificationIcons.PushServiceWaitNetwork: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_notification,
    )

internal actual val NotificationIcons.AlarmPermissionMissing: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_notification,
        inAppNotificationIcon = Notification,
    )
