package net.thunderbird.feature.notification.api.ui.icon

private const val ERROR_MESSAGE = "Can't send notifications from a jvm library. Use android library or app instead."

internal actual val NotificationIcons.AlarmPermissionMissing: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.AuthenticationError: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.CertificateError: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.FailedToCreate: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.MailFetching: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.MailSending: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.MailSendFailed: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.NewMailSingleMail: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.NewMailSummaryMail: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.PushServiceInitializing: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.PushServiceListening: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.PushServiceWaitBackgroundSync: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationIcons.PushServiceWaitNetwork: NotificationIcon get() = error(ERROR_MESSAGE)
