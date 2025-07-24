package net.thunderbird.feature.notification.api.ui.icon

/**
 * Represents a set of icons specifically designed for notifications within the application.
 *
 * This object serves as a namespace for various notification icons, allowing for easy access
 * and organization of these visual assets. Each property within this object is expected to
 * represent a specific notification icon.
 */
internal object NotificationIcons

/**
 * Represents the icon for authentication error notifications.
 *
 * @see net.thunderbird.feature.notification.api.content.AuthenticationErrorNotification
 */
internal expect val NotificationIcons.AuthenticationError: NotificationIcon

/**
 * Represents the icon for the "Certificate Error" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.CertificateErrorNotification
 */
internal expect val NotificationIcons.CertificateError: NotificationIcon

/**
 * Represents the icon for the "Failed To Create notification" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.FailedToCreateNotification
 */
internal expect val NotificationIcons.FailedToCreate: NotificationIcon

/**
 * Represents the icon for the "Mail Fetching" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.MailNotification.Fetching
 */
internal expect val NotificationIcons.MailFetching: NotificationIcon

/**
 * Represents the icon for the "Mail Sending" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.MailNotification.Sending
 */
internal expect val NotificationIcons.MailSending: NotificationIcon

/**
 * Represents the icon for the "Mail Send Failed" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.MailNotification.SendFailed
 */
internal expect val NotificationIcons.MailSendFailed: NotificationIcon

/**
 * Represents the icon for the "New Mail (Single)" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.MailNotification.NewMailSingleMail
 */
internal expect val NotificationIcons.NewMailSingleMail: NotificationIcon

/**
 * Represents the icon for the "New Mail Summary" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.MailNotification.NewMailSummaryMail
 */
internal expect val NotificationIcons.NewMailSummaryMail: NotificationIcon

/**
 * Represents the icon for the "Push Service Initializing" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.PushServiceNotification.Initializing
 */
internal expect val NotificationIcons.PushServiceInitializing: NotificationIcon

/**
 * Represents the icon for the "Push Service Listening" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.PushServiceNotification.Listening
 */
internal expect val NotificationIcons.PushServiceListening: NotificationIcon

/**
 * Represents the icon for the "Push Service Wait Background Sync" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.PushServiceNotification.WaitBackgroundSync
 */
internal expect val NotificationIcons.PushServiceWaitBackgroundSync: NotificationIcon

/**
 * Represents the icon for the "Push Service Wait Network" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.PushServiceNotification.WaitNetwork
 */
internal expect val NotificationIcons.PushServiceWaitNetwork: NotificationIcon

/**
 * Represents the icon for the "Alarm Permission Missing" notification.
 *
 * @see net.thunderbird.feature.notification.api.content.PushServiceNotification.AlarmPermissionMissing
 */
internal expect val NotificationIcons.AlarmPermissionMissing: NotificationIcon
