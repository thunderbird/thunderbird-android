package net.thunderbird.feature.notification.api.content

import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle
import net.thunderbird.feature.notification.api.ui.style.inAppNotificationStyle
import net.thunderbird.feature.notification.resources.api.Res
import net.thunderbird.feature.notification.resources.api.sent_folder_not_found_title
import org.jetbrains.compose.resources.getString

/**
 * A notification that is displayed when the configured 'Sent' folder for an account is not configured.
 *
 * This typically happens when the folder was automatically detected or if it was manually changed to None by the user.
 *
 * The notification prompts the user to assign a new 'Sent' folder for the specified account.
 *
 * @property accountUuid The unique identifier of the account for which the 'Sent' folder is missing.
 * @property title The main title text of the notification, loaded from resources.
 */
@ConsistentCopyVisibility
data class SentFolderNotFoundNotification internal constructor(
    override val accountUuid: String,
    override val title: String,
) : AppNotification(), InAppNotification {
    override val contentText: String = title
    override val severity: NotificationSeverity = NotificationSeverity.Warning
    override val icon: NotificationIcon get() = NotificationIcons.SentFolderNotFound
    override val actions: Set<NotificationAction> = setOf(NotificationAction.AssignSentFolder(accountUuid))
    override val inAppNotificationStyle: InAppNotificationStyle
        // TODO(9572): Properly setup the notification priority.
        get() = inAppNotificationStyle { bannerGlobal(priority = Int.MAX_VALUE) }
}

/**
 * Icon for the 'Sent Folder Not Found' notification.
 */
internal expect val NotificationIcons.SentFolderNotFound: NotificationIcon

/**
 * Factory function to create a [SentFolderNotFoundNotification].
 *
 * @param accountUuid The unique identifier of the account for which the 'Sent' folder is missing.
 * @return A new instance of [SentFolderNotFoundNotification] with the title loaded from string resources.
 */
suspend fun SentFolderNotFoundNotification(
    accountUuid: String,
): SentFolderNotFoundNotification = SentFolderNotFoundNotification(
    accountUuid = accountUuid,
    title = getString(Res.string.sent_folder_not_found_title),
)
