package net.thunderbird.feature.mail.message.list.internal.ui.component

import android.util.TypedValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.theme2.ColorRoles
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.toColorRoles
import app.k9mail.core.ui.compose.theme2.toHarmonizedColor
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.resources.StringRes
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.mail.message.list.internal.R
import net.thunderbird.feature.mail.message.list.R as ApiR

/**
 * Displays the background content for a swipeable message list item, showing an icon and label
 * representing the swipe action being performed.
 *
 * @param action The swipe action to display, determining the icon, label, and color scheme.
 * @param toggled Whether the action is in a toggled state, which may affect the displayed icon and label
 *  for actions like ToggleRead or ToggleStar.
 * @param arrangement The horizontal arrangement of the icon and label within the surface, either Start or End.
 * @param modifier Optional modifier to be applied to the root Surface.
 */
@Composable
fun MessageItemSwipeBackground(
    action: SwipeAction,
    toggled: Boolean,
    arrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier,
) {
    val colorRoles = rememberSwipeActionColorRoles(action, toggled) ?: return
    val iconVector = remember(action, toggled) { action.iconVector(toggled) }
    val actionNameId = remember(action, toggled) { action.actionNameId(toggled) }
    actionNameId ?: return

    Surface(
        modifier = modifier,
        color = colorRoles.accent,
        contentColor = colorRoles.onAccent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MainTheme.spacings.triple),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = arrangement,
        ) {
            val movableIcon = remember(iconVector) {
                movableContentOf {
                    iconVector?.let { iconVector -> Icon(iconVector) }
                }
            }
            if (arrangement == Arrangement.Start) {
                movableIcon()
                Spacer(modifier = Modifier.width(MainTheme.spacings.default))
            }
            TextLabelLarge(stringResource(actionNameId))
            if (arrangement == Arrangement.End) {
                Spacer(modifier = Modifier.width(MainTheme.spacings.default))
                movableIcon()
            }
        }
    }
}

@Composable
private fun rememberSwipeActionColorRoles(action: SwipeAction, toggled: Boolean): ColorRoles? {
    val context = LocalContext.current
    val resources = LocalResources.current
    val primaryColor = MainTheme.colors.primary
    val surfaceContainerLowest = MainTheme.colors.surfaceContainerLowest
    return remember(action, toggled, primaryColor, surfaceContainerLowest) {
        // TODO: move these colours to use the design system whenever we have them available.
        val actionAttrColor = when (action) {
            SwipeAction.None -> return@remember null
            SwipeAction.ToggleSelection -> R.attr.messageListSwipeSelectColor
            SwipeAction.ToggleRead -> R.attr.messageListSwipeToggleReadColor
            SwipeAction.ToggleStar -> R.attr.messageListSwipeToggleStarColor
            SwipeAction.Archive -> R.attr.messageListSwipeArchiveColor
            SwipeAction.ArchiveDisabled, SwipeAction.ArchiveSetupArchiveFolder ->
                null

            SwipeAction.Delete -> R.attr.messageListSwipeDeleteColor
            SwipeAction.Spam -> R.attr.messageListSwipeSpamColor
            SwipeAction.Move -> R.attr.messageListSwipeMoveColor
        }
        val targetColor = if (actionAttrColor != null) {
            val typedValue = TypedValue()
            val found = context.theme.resolveAttribute(actionAttrColor, typedValue, true)
            val colorInt = if (found) {
                typedValue.data
            } else {
                error("Couldn't resolve attribute (${resources.getResourceName(actionAttrColor)}")
            }
            Color(colorInt)
        } else {
            surfaceContainerLowest
        }
        val harmonizedColor = primaryColor.toHarmonizedColor(targetColor)
        harmonizedColor.toColorRoles(context)
    }
}

private fun SwipeAction.iconVector(toggled: Boolean): ImageVector? = when (this) {
    SwipeAction.None -> null
    SwipeAction.ToggleSelection -> Icons.Outlined.CheckCircle
    SwipeAction.ToggleRead if toggled -> Icons.Outlined.MarkEmailUnread
    SwipeAction.ToggleRead -> Icons.Outlined.MarkEmailRead
    SwipeAction.ToggleStar if toggled -> Icons.Outlined.Star
    SwipeAction.ToggleStar -> Icons.Filled.Star
    SwipeAction.ArchiveDisabled -> null
    SwipeAction.Archive, SwipeAction.ArchiveSetupArchiveFolder -> Icons.Outlined.Archive
    SwipeAction.Delete -> Icons.Outlined.Delete
    SwipeAction.Spam -> Icons.Outlined.Report
    SwipeAction.Move -> Icons.Outlined.DriveFileMove
}

@StringRes
private fun SwipeAction.actionNameId(toggled: Boolean): Int? = when (this) {
    SwipeAction.None -> null
    SwipeAction.ToggleSelection if toggled -> ApiR.string.swipe_action_deselect
    SwipeAction.ToggleSelection -> ApiR.string.swipe_action_select
    SwipeAction.ToggleRead if toggled -> ApiR.string.swipe_action_mark_as_unread
    SwipeAction.ToggleRead -> ApiR.string.swipe_action_mark_as_read
    SwipeAction.ToggleStar if toggled -> ApiR.string.swipe_action_remove_star
    SwipeAction.ToggleStar -> ApiR.string.swipe_action_add_star
    SwipeAction.Archive -> ApiR.string.swipe_action_archive
    SwipeAction.ArchiveSetupArchiveFolder -> ApiR.string.swipe_action_archive_folder_not_set
    SwipeAction.ArchiveDisabled -> ApiR.string.swipe_action_change_swipe_gestures
    SwipeAction.Delete -> ApiR.string.swipe_action_delete
    SwipeAction.Spam -> ApiR.string.swipe_action_spam
    SwipeAction.Move -> ApiR.string.swipe_action_move
}
