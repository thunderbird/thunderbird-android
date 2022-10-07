package com.fsck.k9.ui.messagelist

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class MessageListItemAnimator : DefaultItemAnimator() {
    init {
        supportsChangeAnimations = false
        changeDuration = 120
    }

    override fun canReuseUpdatedViewHolder(viewHolder: ViewHolder, payloads: MutableList<Any>): Boolean {
        // ItemTouchHelper expects swiped views to be removed from the view hierarchy. So we don't reuse views that are
        // marked as having been swiped.
        return !viewHolder.wasSwiped && super.canReuseUpdatedViewHolder(viewHolder, payloads)
    }
}
