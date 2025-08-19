package net.thunderbird.feature.notification.api.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton
import app.k9mail.core.ui.compose.designsystem.organism.banner.inline.BannerInlineNotificationCardBehaviour
import app.k9mail.core.ui.compose.designsystem.organism.banner.inline.ErrorBannerInlineNotificationCard
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableSet
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.notification.api.ui.BannerInlineNotificationListHostDefaults.TEST_TAG_BANNER_INLINE_LIST
import net.thunderbird.feature.notification.api.ui.BannerInlineNotificationListHostDefaults.TEST_TAG_CHECK_ERROR_NOTIFICATIONS
import net.thunderbird.feature.notification.api.ui.BannerInlineNotificationListHostDefaults.TEST_TAG_CHECK_ERROR_NOTIFICATIONS_ACTION
import net.thunderbird.feature.notification.api.ui.BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.action.ResolvedNotificationActionButton
import net.thunderbird.feature.notification.api.ui.animation.bannerSlideInSlideOutAnimationSpec
import net.thunderbird.feature.notification.api.ui.host.InAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual
import net.thunderbird.feature.notification.resources.api.Res
import net.thunderbird.feature.notification.resources.api.banner_inline_notification_check_error_notifications
import net.thunderbird.feature.notification.resources.api.banner_inline_notification_open_notifications
import net.thunderbird.feature.notification.resources.api.banner_inline_notification_some_messages_need_attention
import org.jetbrains.compose.resources.stringResource

private const val MAX_VISIBLE_NOTIFICATIONS = 2

/**
 * Displays a list of banner inline notifications.
 *
 * This composable function is responsible for rendering a list of banner inline notifications,
 * which are typically used to display important information or alerts to the user within the app's UI.
 *
 * It observes the state of banner inline notifications from the [hostStateHolder] and updates the UI accordingly.
 * The notifications are displayed with an animation when they appear or disappear.
 *
 * If there are more notifications than the maximum allowed to be displayed ([MAX_VISIBLE_NOTIFICATIONS]),
 * a summary notification is shown, prompting the user to open the full list of error notifications.
 *
 * @param hostStateHolder The [InAppNotificationHostStateHolder] that manages the state of in-app notifications.
 * @param onActionClick A callback function that is invoked when an action button on a notification is clicked.
 * It receives the [NotificationAction] associated with the clicked button.
 * @param onOpenErrorNotificationsClick A callback function that is invoked when the "Open Notifications" button
 * on the summary notification (if shown) is clicked.
 * @param modifier An optional [Modifier] to be applied to the root container of the notification list.
 *
 * @see BannerInlineNotificationListHostLayout
 * @see ErrorBannerInlineNotificationCard
 * @see MAX_VISIBLE_NOTIFICATIONS
 */
@Composable
fun BannerInlineNotificationListHost(
    hostStateHolder: InAppNotificationHostStateHolder,
    onActionClick: (NotificationAction) -> Unit,
    onOpenErrorNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by hostStateHolder.currentInAppNotificationHostState.collectAsState()
    val bannerInlineSet = state.bannerInlineVisuals
    AnimatedContent(
        targetState = bannerInlineSet,
        modifier = modifier.testTagAsResourceId(TEST_TAG_HOST_PARENT),
        transitionSpec = { bannerSlideInSlideOutAnimationSpec() },
    ) { bannerInlineSet ->
        if (bannerInlineSet.isNotEmpty()) {
            BannerInlineNotificationListHostLayout(
                visuals = bannerInlineSet,
                onActionClick = onActionClick,
                onOpenErrorNotificationsClick = onOpenErrorNotificationsClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BannerInlineNotificationListHostLayout(
    visuals: ImmutableSet<BannerInlineVisual>,
    onActionClick: (NotificationAction) -> Unit,
    onOpenErrorNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayableNotifications = remember(visuals) {
        if (visuals.size > MAX_VISIBLE_NOTIFICATIONS) {
            visuals.take(MAX_VISIBLE_NOTIFICATIONS - 1)
        } else {
            visuals
        }
    }
    val leftOver = remember(visuals) { visuals.size - MAX_VISIBLE_NOTIFICATIONS }
    Column(
        modifier = modifier
            .padding(vertical = MainTheme.spacings.half, horizontal = MainTheme.spacings.double)
            .testTagAsResourceId(TEST_TAG_BANNER_INLINE_LIST),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        displayableNotifications.forEachIndexed { index, banner ->
            ErrorBannerInlineNotificationCard(
                title = banner.title,
                supportingText = banner.supportingText,
                actions = {
                    banner.actions.forEachIndexed { actionIndex, action ->
                        ResolvedNotificationActionButton(
                            action = action,
                            onActionClick = onActionClick,
                            modifier = Modifier.testTagAsResourceId(
                                tag = BannerInlineNotificationListHostDefaults.testTagBannerInlineListItemAction(
                                    index = index,
                                    actionIndex = actionIndex,
                                ),
                            ),
                        )
                    }
                },
                behaviour = BannerInlineNotificationCardBehaviour.Clipped,
            )
        }

        if (leftOver > 0) {
            ErrorBannerInlineNotificationCard(
                title = stringResource(resource = Res.string.banner_inline_notification_check_error_notifications),
                supportingText = stringResource(
                    resource = Res.string.banner_inline_notification_some_messages_need_attention,
                ),
                actions = {
                    NotificationActionButton(
                        text = stringResource(
                            resource = Res.string.banner_inline_notification_open_notifications,
                        ),
                        onClick = onOpenErrorNotificationsClick,
                        modifier = Modifier.testTagAsResourceId(TEST_TAG_CHECK_ERROR_NOTIFICATIONS_ACTION),
                    )
                },
                modifier = Modifier.testTagAsResourceId(TEST_TAG_CHECK_ERROR_NOTIFICATIONS),
            )
        }
    }
}

object BannerInlineNotificationListHostDefaults {
    internal const val TEST_TAG_HOST_PARENT = "banner_inline_notification_host"
    internal const val TEST_TAG_BANNER_INLINE_LIST = "banner_inline_notification_list"
    internal const val TEST_TAG_CHECK_ERROR_NOTIFICATIONS = "check_notifications_composable"
    internal const val TEST_TAG_CHECK_ERROR_NOTIFICATIONS_ACTION = "check_notifications_action"

    internal fun testTagBannerInlineListItemAction(index: Int, actionIndex: Int) =
        "banner_inline_notification_list_item_action_${index}_$actionIndex"
}
