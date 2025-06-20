package net.thunderbird.feature.notification.impl.ui.action

import net.thunderbird.feature.notification.api.ui.action.NotificationAction

interface NotificationActionCreator<in TAction : NotificationAction> {
    fun accept(action: NotificationAction): Boolean

    suspend fun create(action: TAction): AndroidNotificationAction

    object TypeQualifier
}
