package com.fsck.k9.ui.messagelist

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.core.graphics.withSave
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.fsck.k9.SwipeAction
import com.fsck.k9.ui.R
import kotlin.math.abs

class MessageListSwipeCallback(
    resources: Resources,
    private val resourceProvider: SwipeResourceProvider,
    private val swipeActionSupportProvider: SwipeActionSupportProvider,
    private val swipeRightAction: SwipeAction,
    private val swipeLeftAction: SwipeAction,
    private val adapter: MessageListAdapter,
    private val listener: MessageListSwipeListener
) : ItemTouchHelper.Callback() {
    private val iconPadding = resources.getDimension(R.dimen.messageListSwipeIconPadding).toInt()
    private val swipeThreshold = resources.getDimension(R.dimen.messageListSwipeThreshold)
    private val backgroundColorPaint = Paint()

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        if (viewHolder !is MessageViewHolder) return 0

        val item = adapter.getItemById(viewHolder.uniqueId) ?: return 0

        var swipeFlags = 0
        if (swipeActionSupportProvider.isActionSupported(item, swipeRightAction)) {
            swipeFlags = swipeFlags or ItemTouchHelper.RIGHT
        }
        if (swipeActionSupportProvider.isActionSupported(item, swipeLeftAction)) {
            swipeFlags = swipeFlags or ItemTouchHelper.LEFT
        }

        return makeMovementFlags(0, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
    ): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        val holder = viewHolder as MessageViewHolder
        val item = adapter.getItemById(holder.uniqueId) ?: error("Couldn't find MessageListItem")

        // ItemTouchHelper expects swiped views to be removed from the view hierarchy. We mark this ViewHolder so that
        // MessageListItemAnimator knows not to reuse it during an animation.
        viewHolder.markAsSwiped(true)

        when (direction) {
            ItemTouchHelper.RIGHT -> listener.onSwipeAction(item, swipeRightAction)
            ItemTouchHelper.LEFT -> listener.onSwipeAction(item, swipeLeftAction)
            else -> error("Unsupported direction: $direction")
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.markAsSwiped(false)
    }

    override fun getSwipeThreshold(viewHolder: ViewHolder): Float {
        return swipeThreshold / viewHolder.itemView.width
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        canvas.withSave {
            val view = viewHolder.itemView

            val holder = viewHolder as MessageViewHolder
            val item = adapter.getItemById(holder.uniqueId) ?: return@withSave

            val swipeThreshold = recyclerView.width * getSwipeThreshold(holder)
            val swipeThresholdReached = abs(dX) > swipeThreshold
            if (swipeThresholdReached) {
                val action = if (dX > 0) swipeRightAction else swipeLeftAction
                val backgroundColor = resourceProvider.getBackgroundColor(item, action)
                drawBackground(view, backgroundColor)
            } else {
                val backgroundColor = resourceProvider.getBackgroundColor(item, SwipeAction.None)
                drawBackground(view, backgroundColor)
            }

            // Stop drawing the icon when the view has been animated all the way off the screen by ItemTouchHelper.
            // We do this so the icon doesn't switch state when RecyclerView's ItemAnimator animates the view back after
            // a toggle action (mark as read/unread, add/remove star) was used.
            if (isCurrentlyActive || abs(dX).toInt() < view.width) {
                drawIcon(dX, view, item, swipeThresholdReached)
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun Canvas.drawBackground(view: View, color: Int) {
        backgroundColorPaint.color = color
        drawRect(
            view.left.toFloat(),
            view.top.toFloat(),
            view.right.toFloat(),
            view.bottom.toFloat(),
            backgroundColorPaint
        )
    }

    private fun Canvas.drawIcon(dX: Float, view: View, item: MessageListItem, swipeThresholdReached: Boolean) {
        if (dX > 0) {
            drawSwipeRightIcon(view, item, swipeThresholdReached)
        } else {
            drawSwipeLeftIcon(view, item, swipeThresholdReached)
        }
    }

    private fun Canvas.drawSwipeRightIcon(view: View, item: MessageListItem, swipeThresholdReached: Boolean) {
        resourceProvider.getIcon(item, swipeRightAction)?.let { icon ->
            val iconLeft = iconPadding
            val iconTop = view.top + ((view.height - icon.intrinsicHeight) / 2)
            val iconRight = iconLeft + icon.intrinsicWidth
            val iconBottom = iconTop + icon.intrinsicHeight
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            icon.setTint(resourceProvider.getIconTint(item, swipeRightAction, swipeThresholdReached))
            icon.draw(this)
        }
    }

    private fun Canvas.drawSwipeLeftIcon(view: View, item: MessageListItem, swipeThresholdReached: Boolean) {
        resourceProvider.getIcon(item, swipeLeftAction)?.let { icon ->
            val iconRight = view.right - iconPadding
            val iconLeft = iconRight - icon.intrinsicWidth
            val iconTop = view.top + ((view.height - icon.intrinsicHeight) / 2)
            val iconBottom = iconTop + icon.intrinsicHeight
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            icon.setTint(resourceProvider.getIconTint(item, swipeLeftAction, swipeThresholdReached))
            icon.draw(this)
        }
    }
}

fun interface SwipeActionSupportProvider {
    fun isActionSupported(item: MessageListItem, action: SwipeAction): Boolean
}

fun interface MessageListSwipeListener {
    fun onSwipeAction(item: MessageListItem, action: SwipeAction)
}

private fun ViewHolder.markAsSwiped(value: Boolean) {
    itemView.setTag(R.id.message_list_swipe_tag, if (value) true else null)
}

val ViewHolder.wasSwiped
    get() = itemView.getTag(R.id.message_list_swipe_tag) == true
