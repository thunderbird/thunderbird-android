package net.thunderbird.feature.notification.api

import net.thunderbird.feature.notification.api.dismisser.NotificationDismisser
import net.thunderbird.feature.notification.api.sender.NotificationSender

/**
 * Manages sending and dismissing notifications.
 *
 * This interface combines the functionalities of [NotificationSender] and [NotificationDismisser]
 * to provide a unified API for notification management.
 */
interface NotificationManager : NotificationSender, NotificationDismisser
