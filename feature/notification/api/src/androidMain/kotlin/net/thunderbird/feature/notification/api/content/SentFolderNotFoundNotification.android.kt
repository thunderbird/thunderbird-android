package net.thunderbird.feature.notification.api.content

import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons

internal actual val NotificationIcons.SentFolderNotFound: NotificationIcon
    get() = NotificationIcon(inAppNotificationIcon = Icons.Outlined.Warning)
