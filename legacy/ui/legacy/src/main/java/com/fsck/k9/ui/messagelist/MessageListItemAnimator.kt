package com.fsck.k9.ui.messagelist

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class MessageListItemAnimator : DefaultItemAnimator() {
    init {
        supportsChangeAnimations = false
        changeDuration = 120
    }

    override fun animateChange(
        oldHolder: ViewHolder,
        newHolder: ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
    ): Boolean {
        if (oldHolder == newHolder && newHolder.wasSwiped) {
            // Don't touch views currently being swiped
            dispatchChangeFinished(oldHolder, true)
            dispatchChangeFinished(newHolder, false)
            return false
        }

        return super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY)
    }
}
