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

class SwipeResourceProvider(private val context: Context) {

    val iconTint = context.resolveColorAttribute(R.attr.messageListSwipeIconTint)

    fun getIcon(item: MessageListItem, action: SwipeAction): Drawable {
        return context.loadDrawable(
            when (action) {
                SwipeAction.None -> error("action == SwipeAction.None")
                SwipeAction.ToggleSelection -> Icons.Outlined.CheckCircle
                SwipeAction.ToggleRead -> if (item.isRead) {
                    Icons.Outlined.MarkEmailUnread
                } else {
                    Icons.Outlined.MarkEmailRead
                }

                SwipeAction.ToggleStar -> if (item.isStarred) {
                    Icons.Outlined.Star
                } else {
                    Icons.Filled.Star
                }

                SwipeAction.Archive -> Icons.Outlined.Archive
                SwipeAction.Delete -> Icons.Outlined.Delete
                SwipeAction.Spam -> Icons.Outlined.Report
                SwipeAction.Move -> Icons.Outlined.DriveFileMove
            },
        )
    }

    @ColorInt
    fun getBackgroundColor(action: SwipeAction): Int {
        return context.resolveColorAttribute(
            when (action) {
                SwipeAction.None -> R.attr.messageListSwipeDisabledBackgroundColor
                SwipeAction.ToggleSelection -> R.attr.messageListSwipeSelectBackgroundColor
                SwipeAction.ToggleRead -> R.attr.messageListSwipeToggleReadBackgroundColor
                SwipeAction.ToggleStar -> R.attr.messageListSwipeToggleStarBackgroundColor
                SwipeAction.Archive -> R.attr.messageListSwipeArchiveBackgroundColor
                SwipeAction.Delete -> R.attr.messageListSwipeDeleteBackgroundColor
                SwipeAction.Spam -> R.attr.messageListSwipeSpamBackgroundColor
                SwipeAction.Move -> R.attr.messageListSwipeMoveBackgroundColor
            },
        )
    }

    fun getActionName(item: MessageListItem, action: SwipeAction, isSelected: Boolean): String {
        return context.loadString(
            when (action) {
                SwipeAction.None -> error("action == SwipeAction.None")
                SwipeAction.ToggleSelection -> if (isSelected) {
                    R.string.swipe_action_deselect
                } else {
                    R.string.swipe_action_select
                }

                SwipeAction.ToggleRead -> if (item.isRead) {
                    R.string.swipe_action_mark_as_unread
                } else {
                    R.string.swipe_action_mark_as_read
                }

                SwipeAction.ToggleStar -> if (item.isStarred) {
                    R.string.swipe_action_remove_star
                } else {
                    R.string.swipe_action_add_star
                }

                SwipeAction.Archive -> R.string.swipe_action_archive
                SwipeAction.Delete -> R.string.swipe_action_delete
                SwipeAction.Spam -> R.string.swipe_action_spam
                SwipeAction.Move -> R.string.swipe_action_move
            },
        )
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
        error("Couldn't resolve attribute ($attrId)")
    }

    return typedValue.data
}

private fun Context.loadString(@StringRes stringResId: Int): String {
    return resources.getString(stringResId)
}
