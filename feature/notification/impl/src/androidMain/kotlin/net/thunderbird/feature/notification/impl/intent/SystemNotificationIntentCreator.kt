package net.thunderbird.feature.notification.impl.intent

import android.app.PendingIntent
import net.thunderbird.feature.notification.api.content.SystemNotification

internal interface SystemNotificationIntentCreator<in TNotification : SystemNotification> {
    fun accept(notification: SystemNotification): Boolean
    fun create(notification: TNotification): PendingIntent

    object TypeQualifier
}
