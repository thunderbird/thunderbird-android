package com.fsck.k9.ui.messagelist

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.SwipeAction
import com.fsck.k9.ui.R
import com.google.android.material.color.ColorRoles
import com.google.android.material.color.MaterialColors
import net.thunderbird.core.android.account.LegacyAccountWrapper

class SwipeResourceProvider(private val context: Context) {

    fun getActionIcon(action: SwipeAction, account: LegacyAccountWrapper): Drawable? {
        val drawableId = when (action) {
            SwipeAction.None -> error("action == SwipeAction.None")
            SwipeAction.ToggleSelection -> Icons.Outlined.CheckCircle
            SwipeAction.ToggleRead -> Icons.Outlined.MarkEmailRead
            SwipeAction.ToggleStar -> Icons.Filled.Star
            SwipeAction.Archive if account.isIncomingServerPop3() -> null
            SwipeAction.Archive -> Icons.Outlined.Archive
            SwipeAction.Delete -> Icons.Outlined.Delete
            SwipeAction.Spam -> Icons.Outlined.Report
            SwipeAction.Move -> Icons.Outlined.DriveFileMove
        }

        return drawableId?.let(context::loadDrawable)
    }

    fun getActionIconToggled(action: SwipeAction): Drawable? {
        return when (action) {
            SwipeAction.None -> error("action == SwipeAction.None")
            SwipeAction.ToggleRead -> context.loadDrawable(Icons.Outlined.MarkEmailUnread)
            SwipeAction.ToggleStar -> context.loadDrawable(Icons.Outlined.Star)

            else -> null
        }
    }

    fun getActionColorRoles(action: SwipeAction, account: LegacyAccountWrapper): ColorRoles {
        val harmonizedColor = MaterialColors.harmonizeWithPrimary(context, getActionColor(action, account))
        return MaterialColors.getColorRoles(context, harmonizedColor)
    }

    @ColorInt
    private fun getActionColor(action: SwipeAction, account: LegacyAccountWrapper): Int {
        return context.resolveColorAttribute(
            when (action) {
                SwipeAction.None -> error("action == SwipeAction.None")
                SwipeAction.ToggleSelection -> R.attr.messageListSwipeSelectColor
                SwipeAction.ToggleRead -> R.attr.messageListSwipeToggleReadColor
                SwipeAction.ToggleStar -> R.attr.messageListSwipeToggleStarColor
                SwipeAction.Archive if account.hasArchiveFolder() ->
                    R.attr.messageListSwipeArchiveColor

                SwipeAction.Archive -> com.google.android.material.R.attr.colorSurfaceContainerLowest
                SwipeAction.Delete -> R.attr.messageListSwipeDeleteColor
                SwipeAction.Spam -> R.attr.messageListSwipeSpamColor
                SwipeAction.Move -> R.attr.messageListSwipeMoveColor
            },
        )
    }

    fun getActionName(action: SwipeAction, account: LegacyAccountWrapper): String {
        return context.loadString(
            when (action) {
                SwipeAction.None -> error("action == SwipeAction.None")
                SwipeAction.ToggleSelection -> R.string.swipe_action_select
                SwipeAction.ToggleRead -> R.string.swipe_action_mark_as_read
                SwipeAction.ToggleStar -> R.string.swipe_action_add_star
                SwipeAction.Archive if account.hasArchiveFolder() && !account.isIncomingServerPop3() ->
                    R.string.swipe_action_archive

                SwipeAction.Archive if !account.isIncomingServerPop3() -> R.string.swipe_action_archive_folder_not_set
                SwipeAction.Archive -> R.string.swipe_action_change_swipe_gestures
                SwipeAction.Delete -> R.string.swipe_action_delete
                SwipeAction.Spam -> R.string.swipe_action_spam
                SwipeAction.Move -> R.string.swipe_action_move
            },
        )
    }

    fun getActionNameToggled(action: SwipeAction): String? {
        return when (action) {
            SwipeAction.None -> error("action == SwipeAction.None")
            SwipeAction.ToggleSelection -> context.loadString(R.string.swipe_action_deselect)
            SwipeAction.ToggleRead -> context.loadString(R.string.swipe_action_mark_as_unread)
            SwipeAction.ToggleStar -> context.loadString(R.string.swipe_action_remove_star)

            else -> null
        }
    }
}

private fun Context.loadDrawable(@DrawableRes drawableResId: Int): Drawable {
    // mutate() is called to ensure that the drawable can be modified and doesn't affect other drawables
    return ResourcesCompat.getDrawable(resources, drawableResId, theme)!!.mutate()
}

fun Context.resolveColorAttribute(attrId: Int): Int {
    val typedValue = TypedValue()

    val found = theme.resolveAttribute(attrId, typedValue, true)
    if (!found) {
        error("Couldn't resolve attribute (${resources.getResourceName(attrId)}")
    }

    return typedValue.data
}

private fun Context.loadString(@StringRes stringResId: Int): String {
    return resources.getString(stringResId)
}
