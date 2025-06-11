package net.thunderbird.feature.notification.api

interface NotificationIdFactory {
    fun next(accountNumber: Int, offset: Int = 0): NotificationId
}
