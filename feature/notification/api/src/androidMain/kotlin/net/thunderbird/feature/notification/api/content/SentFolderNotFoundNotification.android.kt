package net.thunderbird.feature.notification.api.content

import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined.Warning
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons

internal actual val NotificationIcons.SentFolderNotFound: NotificationIcon
    get() = NotificationIcon(inAppNotificationIcon = Icons.Outlined.Warning)
