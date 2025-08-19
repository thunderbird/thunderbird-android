package net.thunderbird.feature.notification.api.ui.host

import kotlinx.collections.immutable.toPersistentSet

enum class DisplayInAppNotificationFlag {
    BannerGlobalNotifications,
    BannerInlineNotifications,
    SnackbarNotifications,
    ;

    companion object {
        val AllNotifications = DisplayInAppNotificationFlag.entries.toPersistentSet()
    }
}
