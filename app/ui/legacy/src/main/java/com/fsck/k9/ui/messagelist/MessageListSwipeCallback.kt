package com.fsck.k9.ui.messagelist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.widget.ImageView
import androidx.core.graphics.withTranslation
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import app.k9mail.ui.utils.itemtouchhelper.ItemTouchHelper
import com.fsck.k9.SwipeAction
import com.fsck.k9.ui.R
import com.google.android.material.color.ColorRoles
import com.google.android.material.textview.MaterialTextView
import kotlin.math.abs

@SuppressLint("InflateParams")
class MessageListSwipeCallback(
    context: Context,
    resourceProvider: SwipeResourceProvider,
    private val swipeActionSupportProvider: SwipeActionSupportProvider,
    private val swipeRightAction: SwipeAction,
    private val swipeLeftAction: SwipeAction,
    private val adapter: MessageListAdapter,
    private val listener: MessageListSwipeListener,
) : ItemTouchHelper.Callback() {
    private val swipePadding = context.resources.getDimension(R.dimen.messageListSwipeIconPadding).toInt()
    private val swipeThreshold = context.resources.getDimension(R.dimen.messageListSwipeThreshold)
    private val backgroundColorPaint = Paint()

    private val swipeRightLayout: View
    private val swipeRightIcon: ImageView
    private val swipeRightText: MaterialTextView
    private val swipeLeftLayout: View
    private val swipeLeftIcon: ImageView
    private val swipeLeftText: MaterialTextView
    private val swipeRightConfig: SwipeActionConfig
    private val swipeLeftConfig: SwipeActionConfig

    private var maxSwipeRightDistance: Int = -1
    private var maxSwipeLeftDistance: Int = -1

    init {
        val layoutInflater = LayoutInflater.from(context)

        swipeRightLayout = layoutInflater.inflate(R.layout.swipe_right_action, null, false)
        swipeLeftLayout = layoutInflater.inflate(R.layout.swipe_left_action, null, false)

        swipeRightIcon = swipeRightLayout.findViewById(R.id.swipe_action_icon)
        swipeRightText = swipeRightLayout.findViewById(R.id.swipe_action_text)

        swipeLeftIcon = swipeLeftLayout.findViewById(R.id.swipe_action_icon)
        swipeLeftText = swipeLeftLayout.findViewById(R.id.swipe_action_text)

        swipeRightConfig = setupSwipeAction(swipeRightAction, resourceProvider)
        swipeLeftConfig = setupSwipeAction(swipeLeftAction, resourceProvider)
    }

    override fun isFlingEnabled(): Boolean {
        return false
    }

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
        target: ViewHolder,
    ): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun onSwipeStarted(viewHolder: ViewHolder, direction: Int) {
        val item = viewHolder.messageListItem ?: return

        // Mark view to prevent MessageListItemAnimator from interfering with swipe animations
        viewHolder.markAsSwiped(true)

        val swipeAction = when (direction) {
            ItemTouchHelper.RIGHT -> swipeRightAction
            ItemTouchHelper.LEFT -> swipeLeftAction
            else -> error("Unsupported direction: $direction")
        }

        listener.onSwipeStarted(item, swipeAction)
    }

    override fun onSwipeDirectionChanged(viewHolder: ViewHolder, direction: Int) {
        val item = viewHolder.messageListItem ?: return

        val swipeAction = when (direction) {
            ItemTouchHelper.RIGHT -> swipeRightAction
            ItemTouchHelper.LEFT -> swipeLeftAction
            else -> error("Unsupported direction: $direction")
        }

        listener.onSwipeActionChanged(item, swipeAction)
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        val item = viewHolder.messageListItem ?: return

        when (direction) {
            ItemTouchHelper.RIGHT -> listener.onSwipeAction(item, swipeRightAction)
            ItemTouchHelper.LEFT -> listener.onSwipeAction(item, swipeLeftAction)
            else -> error("Unsupported direction: $direction")
        }
    }

    override fun onSwipeEnded(viewHolder: ViewHolder) {
        val item = viewHolder.messageListItem ?: return

        listener.onSwipeEnded(item)
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
        isCurrentlyActive: Boolean,
        success: Boolean,
    ) {
        val view = viewHolder.itemView
        val viewWidth = view.width
        val viewHeight = view.height

        if (dX != 0F) {
            canvas.withTranslation(x = view.left.toFloat(), y = view.top.toFloat()) {
                if (isCurrentlyActive || !success) {
                    val holder = viewHolder as MessageViewHolder
                    val item = adapter.getItemById(holder.uniqueId) ?: return@withTranslation
                    drawLayout(dX, viewWidth, viewHeight, item)
                } else {
                    drawBackground(dX, viewWidth, viewHeight)
                }
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive, success)
    }

    private fun Canvas.drawBackground(dX: Float, width: Int, height: Int) {
        val swipeActionConfig = if (dX > 0) swipeRightConfig else swipeLeftConfig

        backgroundColorPaint.color = swipeActionConfig.backgroundColor
        drawRect(
            0F,
            0F,
            width.toFloat(),
            height.toFloat(),
            backgroundColorPaint,
        )
    }

    private fun Canvas.drawLayout(dX: Float, width: Int, height: Int, item: MessageListItem) {
        val swipeRight = dX > 0
        val swipeThresholdReached = abs(dX) > swipeThreshold

        val swipeLayout = if (swipeRight) swipeRightLayout else swipeLeftLayout
        val swipeAction = if (swipeRight) swipeRightAction else swipeLeftAction
        val swipeActionConfig = if (swipeRight) swipeRightConfig else swipeLeftConfig
        val swipeIcon = if (swipeRight) swipeRightIcon else swipeLeftIcon
        val swipeText = if (swipeRight) swipeRightText else swipeLeftText

        val foregroundColor = getForegroundColor(swipeActionConfig, swipeThresholdReached)
        val backgroundColor = getBackgroundColor(swipeActionConfig, swipeThresholdReached)

        swipeLayout.setBackgroundColor(backgroundColor)

        val icon = getIcon(swipeAction, item, swipeActionConfig)
        icon.setTint(foregroundColor)

        swipeIcon.setImageDrawable(icon)

        val isSelected = adapter.isSelected(item)
        swipeText.text = getActionName(swipeAction, item, isSelected, swipeActionConfig)
        swipeText.setTextColor(foregroundColor)

        if (swipeLayout.isDirty) {
            val widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            swipeLayout.measure(widthMeasureSpec, heightMeasureSpec)
            swipeLayout.layout(0, 0, width, height)

            if (swipeRight) {
                maxSwipeRightDistance = swipeText.right + swipePadding
            } else {
                maxSwipeLeftDistance = swipeLayout.width - swipeText.left + swipePadding
            }
        }

        swipeLayout.draw(this)
    }

    private fun getActionName(
        swipeAction: SwipeAction,
        item: MessageListItem,
        isSelected: Boolean,
        swipeActionConfig: SwipeActionConfig,
    ) = if (isToggled(swipeAction, item) || isSelected) {
        swipeActionConfig.actionNameToggled ?: error("action has no toggled name")
    } else {
        swipeActionConfig.actionName
    }

    private fun getIcon(
        swipeAction: SwipeAction,
        item: MessageListItem,
        swipeActionConfig: SwipeActionConfig,
    ) = if (isToggled(swipeAction, item)) {
        swipeActionConfig.iconToggled ?: error("action has no toggled icon")
    } else {
        swipeActionConfig.icon
    }

    private fun getForegroundColor(
        swipeActionConfig: SwipeActionConfig,
        swipeThresholdReached: Boolean,
    ) = if (swipeThresholdReached) {
        swipeActionConfig.backgroundColor
    } else {
        swipeActionConfig.foregroundColor
    }

    private fun getBackgroundColor(
        swipeActionConfig: SwipeActionConfig,
        swipeThresholdReached: Boolean,
    ) = if (swipeThresholdReached) {
        swipeActionConfig.foregroundColor
    } else {
        swipeActionConfig.backgroundColor
    }

    override fun getMaxSwipeDistance(recyclerView: RecyclerView, direction: Int): Int {
        return when (direction) {
            ItemTouchHelper.RIGHT -> if (maxSwipeRightDistance > 0) maxSwipeRightDistance else recyclerView.width
            ItemTouchHelper.LEFT -> if (maxSwipeLeftDistance > 0) maxSwipeLeftDistance else recyclerView.width
            else -> recyclerView.width
        }
    }

    override fun shouldAnimateOut(direction: Int): Boolean {
        return when (direction) {
            ItemTouchHelper.RIGHT -> swipeRightAction.removesItem
            ItemTouchHelper.LEFT -> swipeLeftAction.removesItem
            else -> error("Unsupported direction")
        }
    }

    override fun getAnimationDuration(
        recyclerView: RecyclerView,
        animationType: Int,
        animateDx: Float,
        animateDy: Float,
    ): Long {
        val percentage = abs(animateDx) / recyclerView.width
        return (super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy) * percentage).toLong()
    }

    private fun setupSwipeAction(swipeAction: SwipeAction, resourceProvider: SwipeResourceProvider) = SwipeActionConfig(
        colorRoles = resourceProvider.getActionColorRoles(swipeAction),
        icon = resourceProvider.getActionIcon(swipeAction),
        iconToggled = resourceProvider.getActionIconToggled(swipeAction),
        actionName = resourceProvider.getActionName(swipeAction),
        actionNameToggled = resourceProvider.getActionNameToggled(swipeAction),
    )

    private fun isToggled(swipeAction: SwipeAction, item: MessageListItem): Boolean {
        return when (swipeAction) {
            SwipeAction.ToggleRead -> item.isRead
            SwipeAction.ToggleStar -> item.isStarred
            else -> false
        }
    }

    private val ViewHolder.messageListItem: MessageListItem?
        get() = (this as? MessageViewHolder)?.uniqueId?.let { adapter.getItemById(it) }
}

fun interface SwipeActionSupportProvider {
    fun isActionSupported(item: MessageListItem, action: SwipeAction): Boolean
}

interface MessageListSwipeListener {
    fun onSwipeStarted(item: MessageListItem, action: SwipeAction)
    fun onSwipeActionChanged(item: MessageListItem, action: SwipeAction)
    fun onSwipeAction(item: MessageListItem, action: SwipeAction)
    fun onSwipeEnded(item: MessageListItem)
}

private data class SwipeActionConfig(
    private val colorRoles: ColorRoles,
    val icon: Drawable,
    val iconToggled: Drawable? = null,
    val actionName: String,
    val actionNameToggled: String? = null,
) {
    val foregroundColor = colorRoles.accent
    val backgroundColor = colorRoles.onAccent
}

private fun ViewHolder.markAsSwiped(value: Boolean) {
    itemView.setTag(R.id.message_list_swipe_tag, if (value) true else null)
}

val ViewHolder.wasSwiped
    get() = itemView.getTag(R.id.message_list_swipe_tag) == true
