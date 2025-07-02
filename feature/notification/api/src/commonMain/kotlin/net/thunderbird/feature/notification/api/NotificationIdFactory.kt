package net.thunderbird.feature.notification.api

// TODO(#9416): Migrate logic from NotificationIds to NotificationIdFactory
interface NotificationIdFactory {
    fun next(accountNumber: Int, offset: Int = 0): NotificationId
}
