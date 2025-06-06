package net.thunderbird.feature.notification

import net.thunderbird.feature.notification.resources.Res
import net.thunderbird.feature.notification.resources.notification_channel_messages_description
import net.thunderbird.feature.notification.resources.notification_channel_messages_title
import net.thunderbird.feature.notification.resources.notification_channel_miscellaneous_description
import net.thunderbird.feature.notification.resources.notification_channel_miscellaneous_title
import net.thunderbird.feature.notification.resources.notification_channel_push_description
import net.thunderbird.feature.notification.resources.notification_channel_push_title
import org.jetbrains.compose.resources.StringResource

/**
 * Represents the different notification channels used by the application.
 *
 * Each sealed class variant defines a specific type of notification channel with its unique [id].
 *
 * @property id The unique identifier for the notification channel.
 * @property name The user-visible name of the channel.
 * @property description The user-visible description of the channel.
 * @property importance The importance level of the channel.
 */
sealed class NotificationChannel(
    val id: String,
    val name: StringResource,
    val description: StringResource,
    val importance: NotificationChannelImportance,
) {
    /**
     * Represents a notification channel for new messages.
     *
     * @property accountUuid The unique identifier of the account associated with these messages.
     * @property suffix An optional suffix to further differentiate the channel, e.g., for different folder types.
     */
    data class Messages(
        val accountUuid: String,
        val suffix: String,
    ) : NotificationChannel(
        id = "messages_channel_$accountUuid$suffix",
        name = Res.string.notification_channel_messages_title,
        description = Res.string.notification_channel_messages_description,
        importance = NotificationChannelImportance.Default,
    )

    /**
     * Represents a notification channel for miscellaneous notifications.
     *
     * This channel is used for notifications that don't fit into other specific categories.
     * The channel ID is "misc" if no account is specified, or "miscellaneous_channel_[accountUuid]" if an
     * account is provided.
     *
     * @property accountUuid The unique identifier of the account associated with these notifications, if applicable.
     */
    data class Miscellaneous(
        val accountUuid: String? = null,
    ) : NotificationChannel(
        id = if (accountUuid.isNullOrBlank()) {
            "misc"
        } else {
            "miscellaneous_channel_$accountUuid"
        },
        name = Res.string.notification_channel_miscellaneous_title,
        description = Res.string.notification_channel_miscellaneous_description,
        importance = NotificationChannelImportance.Low,
    )

    /**
     * Represents a notification channel for push service messages.
     *
     * This channel is used for notifications related to the background push service,
     * such as connection status or errors.
     */
    data object PushService : NotificationChannel(
        id = "push",
        name = Res.string.notification_channel_push_title,
        description = Res.string.notification_channel_push_description,
        importance = NotificationChannelImportance.Low,
    )
}

/**
 * Represents the importance level of a notification channel.
 *
 * These levels correspond to the Android notification channel importance constants.
 * @see NotificationChannelImportance.None
 * @see NotificationChannelImportance.Min
 * @see NotificationChannelImportance.Low
 * @see NotificationChannelImportance.Default
 * @see NotificationChannelImportance.High
 */
enum class NotificationChannelImportance {
    /**
     * A notification with no importance: does not show in the notification panel.
     */
    None,

    /**
     * Min notification importance: only shows in the notification panel, below the fold.
     *
     * **Android**: This should not be used with `Service.startForeground` since a foreground service is
     * supposed to be something the user cares about so it does not make semantic sense to mark its notification
     * as minimum importance.
     *
     * If you do this as of Android version `Build.VERSION_CODES.O`, the system will show a higher-priority
     * notification about your app running in the background.
     */
    Min,

    /**
     * Low notification importance: Shows in the shade, and potentially in the status bar, but is not
     * audibly intrusive.
     */
    Low,

    /**
     * Default notification importance: shows everywhere, makes noise, but does not visually intrude.
     */
    Default,

    /**
     * Higher notification importance: shows everywhere, makes noise and peeks. May use full screen intents.
     */
    High,
}
