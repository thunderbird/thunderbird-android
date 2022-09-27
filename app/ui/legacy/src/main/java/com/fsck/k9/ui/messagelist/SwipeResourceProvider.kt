package com.fsck.k9.ui.messagelist

import android.content.res.Resources.Theme
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import com.fsck.k9.SwipeAction
import com.fsck.k9.ui.R
import com.fsck.k9.ui.resolveColorAttribute
import com.fsck.k9.ui.resolveDrawableAttribute

class SwipeResourceProvider(val theme: Theme) {
    private val iconTint = theme.resolveColorAttribute(R.attr.messageListSwipeIconTint)

    private val selectIcon = theme.loadDrawable(R.attr.messageListSwipeSelectIcon)
    private val markAsReadIcon = theme.loadDrawable(R.attr.messageListSwipeMarkAsReadIcon)
    private val markAsUnreadIcon = theme.loadDrawable(R.attr.messageListSwipeMarkAsUnreadIcon)
    private val addStarIcon = theme.loadDrawable(R.attr.messageListSwipeAddStarIcon)
    private val removeStarIcon = theme.loadDrawable(R.attr.messageListSwipeRemoveStarIcon)
    private val archiveIcon = theme.loadDrawable(R.attr.messageListSwipeArchiveIcon)
    private val deleteIcon = theme.loadDrawable(R.attr.messageListSwipeDeleteIcon)
    private val spamIcon = theme.loadDrawable(R.attr.messageListSwipeSpamIcon)
    private val moveIcon = theme.loadDrawable(R.attr.messageListSwipeMoveIcon)

    private val noActionColor = theme.resolveColorAttribute(R.attr.messageListSwipeDisabledBackgroundColor)
    private val selectColor = theme.resolveColorAttribute(R.attr.messageListSwipeSelectBackgroundColor)
    private val markAsReadColor = theme.resolveColorAttribute(R.attr.messageListSwipeMarkAsReadBackgroundColor)
    private val markAsUnreadColor = theme.resolveColorAttribute(R.attr.messageListSwipeMarkAsUnreadBackgroundColor)
    private val addStarColor = theme.resolveColorAttribute(R.attr.messageListSwipeAddStarBackgroundColor)
    private val removeStarColor = theme.resolveColorAttribute(R.attr.messageListSwipeRemoveStarBackgroundColor)
    private val archiveColor = theme.resolveColorAttribute(R.attr.messageListSwipeArchiveBackgroundColor)
    private val deleteColor = theme.resolveColorAttribute(R.attr.messageListSwipeDeleteBackgroundColor)
    private val spamColor = theme.resolveColorAttribute(R.attr.messageListSwipeSpamBackgroundColor)
    private val moveColor = theme.resolveColorAttribute(R.attr.messageListSwipeMoveBackgroundColor)

    fun getIconTint(item: MessageListItem, action: SwipeAction): Int = iconTint

    fun getIcon(item: MessageListItem, action: SwipeAction): Drawable? {
        return when (action) {
            SwipeAction.None -> null
            SwipeAction.ToggleSelection -> selectIcon
            SwipeAction.ToggleRead -> if (item.isRead) markAsUnreadIcon else markAsReadIcon
            SwipeAction.ToggleStar -> if (item.isStarred) removeStarIcon else addStarIcon
            SwipeAction.Archive -> archiveIcon
            SwipeAction.Delete -> deleteIcon
            SwipeAction.Spam -> spamIcon
            SwipeAction.Move -> moveIcon
        }
    }

    fun getBackgroundColor(item: MessageListItem, action: SwipeAction): Int {
        return when (action) {
            SwipeAction.None -> noActionColor
            SwipeAction.ToggleSelection -> selectColor
            SwipeAction.ToggleRead -> if (item.isRead) markAsUnreadColor else markAsReadColor
            SwipeAction.ToggleStar -> if (item.isStarred) removeStarColor else addStarColor
            SwipeAction.Archive -> archiveColor
            SwipeAction.Delete -> deleteColor
            SwipeAction.Spam -> spamColor
            SwipeAction.Move -> moveColor
        }
    }
}

private fun Theme.loadDrawable(@AttrRes attributeId: Int): Drawable {
    return resolveDrawableAttribute(attributeId).mutate()
}
