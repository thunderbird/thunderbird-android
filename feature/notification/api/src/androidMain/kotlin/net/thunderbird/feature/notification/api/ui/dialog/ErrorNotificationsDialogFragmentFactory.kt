package net.thunderbird.feature.notification.api.ui.dialog

import androidx.fragment.app.FragmentManager
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

fun interface ErrorNotificationsDialogFragmentFactory {
    fun show(fragmentManager: FragmentManager)
}

fun interface ErrorNotificationsDialogFragmentActionListener {
    fun onNotificationActionClick(action: NotificationAction)
}
