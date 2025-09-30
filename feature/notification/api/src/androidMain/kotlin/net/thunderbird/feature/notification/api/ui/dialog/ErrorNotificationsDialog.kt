package net.thunderbird.feature.notification.api.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.designsystem.organism.banner.inline.BannerInlineNotificationCardBehaviour
import app.k9mail.core.ui.compose.designsystem.organism.banner.inline.ErrorBannerInlineNotificationCard
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableSet
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.action.ResolvedNotificationActionButton
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual

@Composable
fun ErrorNotificationsDialog(
    visuals: ImmutableSet<BannerInlineVisual>,
    onDismiss: () -> Unit,
    onNotificationActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = ErrorNotificationsDialogDefaults.defaultProperties,
) {
    val showDialog = remember(visuals) { visuals.isNotEmpty() }
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = properties,
        ) {
            ErrorNotificationsDialogContent(
                bannerInlineVisuals = visuals,
                onDismiss = onDismiss,
                onNotificationActionClick = onNotificationActionClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ErrorNotificationsDialogContent(
    bannerInlineVisuals: ImmutableSet<BannerInlineVisual>,
    onDismiss: () -> Unit,
    onNotificationActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier) {
        Column {
            TopAppBar(
                title = "Error Notifications",
                navigationIcon = {
                    ButtonIcon(onClick = onDismiss, imageVector = Icons.Outlined.Close)
                },
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
                contentPadding = PaddingValues(
                    top = MainTheme.spacings.default,
                    start = MainTheme.spacings.double,
                    end = MainTheme.spacings.double,
                    bottom = MainTheme.spacings.double,
                ),
            ) {
                items(
                    count = bannerInlineVisuals.size,
                ) { index ->
                    val item = bannerInlineVisuals.elementAt(index)
                    ErrorBannerInlineNotificationCard(
                        title = item.title,
                        supportingText = item.supportingText,
                        actions = {
                            item.actions.forEachIndexed { actionIndex, action ->
                                ResolvedNotificationActionButton(
                                    action = action,
                                    onActionClick = onNotificationActionClick,
                                    modifier = Modifier.testTagAsResourceId(
                                        tag = ErrorNotificationsDialogDefaults.testTagBannerInlineListItemAction(
                                            index = index,
                                            actionIndex = actionIndex,
                                        ),
                                    ),
                                )
                            }
                        },
                        behaviour = BannerInlineNotificationCardBehaviour.Expanded,
                    )
                }
            }
        }
    }
}

object ErrorNotificationsDialogDefaults {
    const val DEFAULT_TAG = "error_notifications_dialog"

    val defaultProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    )

    internal fun testTagBannerInlineListItemAction(index: Int, actionIndex: Int) =
        "${DEFAULT_TAG}_banner_inline_notification_list_item_action_${index}_$actionIndex"
}
