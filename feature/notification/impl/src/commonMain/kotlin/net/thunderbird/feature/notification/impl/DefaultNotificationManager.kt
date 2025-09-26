package net.thunderbird.feature.notification.impl

import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.dismisser.NotificationDismisser
import net.thunderbird.feature.notification.api.sender.NotificationSender

/**
 * Default implementation of [NotificationManager].
 *
 * This class acts as a central point for managing notifications, delegating sending and dismissing
 * operations to the provided [NotificationSender] and [NotificationDismisser] respectively.
 *
 * @param notificationSender The [NotificationSender] responsible for displaying notifications.
 * @param notificationDismisser The [NotificationDismisser] responsible for removing notifications.
 */
class DefaultNotificationManager(
    private val notificationSender: NotificationSender,
    private val notificationDismisser: NotificationDismisser,
) : NotificationManager, NotificationSender by notificationSender, NotificationDismisser by notificationDismisser
