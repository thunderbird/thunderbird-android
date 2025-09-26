package net.thunderbird.feature.notification.api.ui.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton
import org.jetbrains.compose.resources.stringResource

/**
 * Displays a [NotificationActionButton] with the title resolved from the [NotificationAction].
 *
 * This composable function takes a [NotificationAction] and resolves its title asynchronously.
 * While the title is being resolved, nothing is displayed. Once the title is available,
 * it renders a [NotificationActionButton] with the resolved title.
 *
 * @param action The [NotificationAction] to display.
 * @param onActionClick Callback invoked when the action button is clicked.
 * @param modifier Optional [Modifier] to be applied to the composable.
 */
@Composable
internal fun ResolvedNotificationActionButton(
    action: NotificationAction,
    onActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    NotificationActionButton(
        text = when (val labelResource = action.labelResource) {
            null if action.label.isNotEmpty() -> action.label
            null -> error(
                "You must specify at least one of labelResource or label in ${
                    action::class.simpleName
                }",
            )

            else -> stringResource(labelResource)
        },
        onClick = { onActionClick(action) },
        isExternalLink = action is NotificationAction.ViewSupportArticle,
        modifier = modifier,
    )
}
