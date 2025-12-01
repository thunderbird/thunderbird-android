package net.thunderbird.feature.notification.api.content

import net.thunderbird.feature.notification.api.ui.icon.ERROR_MESSAGE
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons

internal actual val NotificationIcons.SentFolderNotFound: NotificationIcon get() = error(ERROR_MESSAGE)
