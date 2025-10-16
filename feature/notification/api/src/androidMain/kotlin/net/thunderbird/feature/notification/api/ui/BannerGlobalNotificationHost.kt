package net.thunderbird.feature.notification.api.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.ErrorBannerGlobalNotificationCard
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.InfoBannerGlobalNotificationCard
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.WarningBannerGlobalNotificationCard
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.BannerGlobalNotificationHostDefaults.TEST_TAG_BANNER_GLOBAL_ACTION
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.action.ResolvedNotificationActionButton
import net.thunderbird.feature.notification.api.ui.animation.bannerSlideInSlideOutAnimationSpec
import net.thunderbird.feature.notification.api.ui.host.InAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.host.visual.BannerGlobalVisual

/**
 * Displays global notifications as banners.
 *
 * This Composable observes the [InAppNotificationHostStateHolder] for changes in the
 * [BannerGlobalVisual] and displays the appropriate banner notification with an animation.
 *
 * @param hostStateHolder The [InAppNotificationHostStateHolder] that holds the current notification state.
 * @param onActionClick A callback that is invoked when a notification action button is clicked.
 * @param modifier Optional [Modifier] to be applied to the banner host.
 */
@Composable
fun BannerGlobalNotificationHost(
    hostStateHolder: InAppNotificationHostStateHolder,
    onActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by hostStateHolder.currentInAppNotificationHostState.collectAsState()
    val bannerGlobal = state.bannerGlobalVisual
    AnimatedContent(
        targetState = bannerGlobal,
        modifier = modifier.testTagAsResourceId(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST),
        transitionSpec = { bannerSlideInSlideOutAnimationSpec() },
    ) { bannerGlobal ->
        if (bannerGlobal != null) {
            BannerGlobalNotificationHostLayout(
                visual = bannerGlobal,
                onActionClick = onActionClick,
            )
        }
    }
}

@Composable
private fun BannerGlobalNotificationHostLayout(
    visual: BannerGlobalVisual,
    onActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val action = remember(visual.action) {
        movableContentOf {
            visual.action?.let { action ->
                ResolvedNotificationActionButton(
                    action = action,
                    onActionClick = onActionClick,
                    modifier = Modifier.testTagAsResourceId(TEST_TAG_BANNER_GLOBAL_ACTION),
                )
            }
        }
    }

    when (visual.severity) {
        NotificationSeverity.Fatal, NotificationSeverity.Critical -> ErrorBannerGlobalNotificationCard(
            text = visual.message,
            action = action,
            modifier = modifier.testTagAsResourceId(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER),
        )

        NotificationSeverity.Warning -> WarningBannerGlobalNotificationCard(
            text = visual.message,
            action = action,
            modifier = modifier.testTagAsResourceId(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER),
        )

        NotificationSeverity.Temporary, NotificationSeverity.Information -> InfoBannerGlobalNotificationCard(
            text = visual.message,
            action = action,
            modifier = modifier.testTagAsResourceId(BannerGlobalNotificationHostDefaults.TEST_TAG_INFO_BANNER),
        )
    }
}

object BannerGlobalNotificationHostDefaults {
    internal const val TEST_TAG_HOST = "banner_global_notification_host"
    internal const val TEST_TAG_ERROR_BANNER = "error_banner_global_notification"
    internal const val TEST_TAG_WARNING_BANNER = "warning_banner_global_notification"
    internal const val TEST_TAG_INFO_BANNER = "info_banner_global_notification"
    internal const val TEST_TAG_BANNER_GLOBAL_ACTION = "banner_global_action"
}
