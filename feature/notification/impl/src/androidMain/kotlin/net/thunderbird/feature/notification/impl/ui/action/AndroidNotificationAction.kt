package net.thunderbird.feature.notification.impl.ui.action

import android.app.PendingIntent
import androidx.annotation.DrawableRes

/**
 * Represents an action that can be performed on an Android notification.
 *
 * @property icon The drawable resource ID for the action's icon.
 * @property title The title of the action.
 * @property pendingIntent The [PendingIntent] to be executed when the action is triggered.
 */
data class AndroidNotificationAction(
    @DrawableRes
    val icon: Int,
    val title: String,
    val pendingIntent: PendingIntent?,
)
