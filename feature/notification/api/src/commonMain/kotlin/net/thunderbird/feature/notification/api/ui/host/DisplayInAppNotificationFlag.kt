package net.thunderbird.feature.notification.api.ui.host

@JvmInline
value class DisplayInAppNotificationFlag(private val mask: Int) {
    companion object {
        private const val IN_APP_NOTIFICATIONS_ENABLED_FLAG = 1

        // !! IMPORTANT !!: Whenever adding a new visual, increase the flag size.
        private const val IN_APP_NOTIFICATIONS_ENABLED_FLAG_SIZE = 3

        val None = DisplayInAppNotificationFlag(mask = 0)
        val BannerGlobalNotifications = DisplayInAppNotificationFlag(mask = IN_APP_NOTIFICATIONS_ENABLED_FLAG shl 1)
        val BannerInlineNotifications = DisplayInAppNotificationFlag(mask = IN_APP_NOTIFICATIONS_ENABLED_FLAG shl 2)
        val SnackbarNotifications = DisplayInAppNotificationFlag(mask = IN_APP_NOTIFICATIONS_ENABLED_FLAG shl 3)
        val AllNotifications = DisplayInAppNotificationFlag(
            mask = (IN_APP_NOTIFICATIONS_ENABLED_FLAG_SIZE + 1).let { shift ->
                IN_APP_NOTIFICATIONS_ENABLED_FLAG shl shift
            } - 1,
        )
    }

    infix fun or(other: DisplayInAppNotificationFlag): DisplayInAppNotificationFlag =
        DisplayInAppNotificationFlag(mask or other.mask)

    internal infix fun and(other: DisplayInAppNotificationFlag): DisplayInAppNotificationFlag =
        DisplayInAppNotificationFlag(mask and other.mask)
}
