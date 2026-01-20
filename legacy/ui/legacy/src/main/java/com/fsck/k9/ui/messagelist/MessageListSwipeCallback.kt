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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import app.k9mail.ui.utils.itemtouchhelper.ItemTouchHelper
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messagelist.item.ComposableMessageViewHolder
import com.fsck.k9.ui.messagelist.item.MessageViewHolder
import com.google.android.material.color.ColorRoles
import com.google.android.material.textview.MaterialTextView
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.feature.account.AccountId

@SuppressLint("InflateParams")
@Suppress("LongParameterList")
class MessageListSwipeCallback(
    context: Context,
    scope: CoroutineScope,
    private val resourceProvider: SwipeResourceProvider,
    private val swipeActionSupportProvider: SwipeActionSupportProvider,
    private val swipeActions: StateFlow<Map<AccountId, SwipeActions>>,
    private val adapter: MessageListAdapter,
    private val listener: MessageListSwipeListener,
    accounts: List<LegacyAccount>,
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
    private val swipeRightConfig: MutableMap<AccountId, SwipeActionConfig> = mutableMapOf()
    private val swipeLeftConfig: MutableMap<AccountId, SwipeActionConfig> = mutableMapOf()

    private var maxSwipeRightDistance: Int = -1
    private var maxSwipeLeftDistance: Int = -1
    private var activeSwipingMessageListItem: MessageListItem? = null

    init {
        val layoutInflater = LayoutInflater.from(context)

        swipeRightLayout = layoutInflater.inflate(R.layout.swipe_right_action, null, false)
        swipeLeftLayout = layoutInflater.inflate(R.layout.swipe_left_action, null, false)

        swipeRightIcon = swipeRightLayout.findViewById(R.id.swipe_action_icon)
        swipeRightText = swipeRightLayout.findViewById(R.id.swipe_action_text)

        swipeLeftIcon = swipeLeftLayout.findViewById(R.id.swipe_action_icon)
        swipeLeftText = swipeLeftLayout.findViewById(R.id.swipe_action_text)

        swipeActions
            .onEach { swipeActions ->
                swipeLeftConfig.apply {
                    clear()
                    putAll(
                        swipeActions.setupSwipeActionConfig(
                            accounts = accounts,
                            resourceProvider = resourceProvider,
                            isLeft = true,
                        ),
                    )
                }
                swipeRightConfig.apply {
                    clear()
                    putAll(
                        swipeActions.setupSwipeActionConfig(
                            accounts = accounts,
                            resourceProvider = resourceProvider,
                            isLeft = false,
                        ),
                    )
                }
            }
            .launchIn(scope)
    }

    override fun isFlingEnabled(): Boolean {
        return false
    }

    private val MessageListItem.swipeActions: SwipeActions
        get() {
            val swipeActions = this@MessageListSwipeCallback.swipeActions
            return requireNotNull(swipeActions.value[account.id]) {
                "Could not find swipe actions for account ${account.uuid}. swipeActions = $swipeActions"
            }
        }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val item = viewHolder.messageListItem ?: return 0
        val (swipeLeftAction, swipeRightAction) = item.swipeActions

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
        activeSwipingMessageListItem = item

        // Mark view to prevent MessageListItemAnimator from interfering with swipe animations
        viewHolder.markAsSwiped(true)

        val (swipeLeftAction, swipeRightAction) = item.swipeActions
        val swipeAction = when (direction) {
            ItemTouchHelper.RIGHT -> swipeRightAction
            ItemTouchHelper.LEFT -> swipeLeftAction
            else -> error("Unsupported direction: $direction")
        }

        listener.onSwipeStarted(item, swipeAction)
    }

    override fun onSwipeDirectionChanged(viewHolder: ViewHolder, direction: Int) {
        val item = viewHolder.messageListItem ?: return

        val (swipeLeftAction, swipeRightAction) = item.swipeActions
        val swipeAction = when (direction) {
            ItemTouchHelper.RIGHT -> swipeRightAction
            ItemTouchHelper.LEFT -> swipeLeftAction
            else -> error("Unsupported direction: $direction")
        }

        listener.onSwipeActionChanged(item, swipeAction)
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        val item = viewHolder.messageListItem ?: return

        val (swipeLeftAction, swipeRightAction) = item.swipeActions
        when (direction) {
            ItemTouchHelper.RIGHT -> listener.onSwipeAction(item, swipeRightAction)
            ItemTouchHelper.LEFT -> listener.onSwipeAction(item, swipeLeftAction)
            else -> error("Unsupported direction: $direction")
        }
    }

    override fun onSwipeEnded(viewHolder: ViewHolder) {
        val item = viewHolder.messageListItem ?: return

        listener.onSwipeEnded(item)
        activeSwipingMessageListItem = null
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
                val item = viewHolder.messageListItem ?: return@withTranslation
                if (isCurrentlyActive || !success) {
                    drawLayout(dX, viewWidth, viewHeight, item)
                } else {
                    drawBackground(dX, viewWidth, viewHeight, item)
                }
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive, success)
    }

    private fun Canvas.drawBackground(dX: Float, width: Int, height: Int, item: MessageListItem) {
        val swipeActionConfig = if (dX > 0) swipeRightConfig else swipeLeftConfig
        if (swipeActionConfig[item.account.id] == null) {
            error("drawBackground() called despite swipeActionConfig[item.accountWrapper] == null")
        }

        backgroundColorPaint.color = swipeActionConfig.getValue(item.account.id).backgroundColor
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
        val account = item.account

        val swipeActionConfig = if (swipeRight) swipeRightConfig[account.id] else swipeLeftConfig[account.id]
        if (swipeActionConfig == null) {
            error(
                "drawLayout() called despite swipeActionConfig == null. " +
                    "\nswipeActions = ${swipeActions.value}, " +
                    "\nswipeRightConfig = $swipeRightConfig, " +
                    "\nswipeLeftConfig = $swipeLeftConfig",
            )
        }

        val (swipeLeftAction, swipeRightAction) = item.swipeActions
        val swipeLayout = if (swipeRight) swipeRightLayout else swipeLeftLayout
        val swipeAction = if (swipeRight) swipeRightAction else swipeLeftAction
        val swipeIcon = if (swipeRight) swipeRightIcon else swipeLeftIcon
        val swipeText = if (swipeRight) swipeRightText else swipeLeftText

        val foregroundColor = getForegroundColor(swipeActionConfig, swipeThresholdReached)
        val backgroundColor = getBackgroundColor(swipeActionConfig, swipeThresholdReached)

        swipeLayout.setBackgroundColor(backgroundColor)

        val icon = getIcon(swipeAction, item, swipeActionConfig)
        icon?.setTint(foregroundColor)
        swipeIcon.setImageDrawable(icon)
        swipeIcon.isVisible = icon != null

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
    ): Drawable? = if (isToggled(swipeAction, item)) {
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
        val swipeActions = activeSwipingMessageListItem
            ?.let { swipeActions.value[it.account.id] } ?: return false

        val swipeAction = when (direction) {
            ItemTouchHelper.RIGHT -> swipeActions.rightAction
            ItemTouchHelper.LEFT -> swipeActions.leftAction
            else -> error("Unsupported direction")
        }

        return swipeAction.removesItem
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

    private fun Map<AccountId, SwipeActions>.setupSwipeActionConfig(
        accounts: List<LegacyAccount>,
        resourceProvider: SwipeResourceProvider,
        isLeft: Boolean,
    ): Map<AccountId, SwipeActionConfig> {
        return accounts
            .mapNotNull { account ->
                this[account.id]
                    ?.let { (swipeActionLeft, swipeActionRight) ->
                        if (isLeft) swipeActionLeft else swipeActionRight
                    }
                    ?.takeIf { swipeAction -> swipeAction != SwipeAction.None }
                    ?.let { swipeAction -> account.id to swipeAction }
            }
            .associate { (id, swipeAction) ->
                id to SwipeActionConfig(
                    colorRoles = resourceProvider.getActionColorRoles(swipeAction),
                    icon = resourceProvider.getActionIcon(swipeAction),
                    iconToggled = resourceProvider.getActionIconToggled(swipeAction),
                    actionName = resourceProvider.getActionName(swipeAction),
                    actionNameToggled = resourceProvider.getActionNameToggled(swipeAction),
                )
            }
    }

    private fun isToggled(swipeAction: SwipeAction, item: MessageListItem): Boolean {
        return when (swipeAction) {
            SwipeAction.ToggleRead -> item.isRead
            SwipeAction.ToggleStar -> item.isStarred
            else -> false
        }
    }

    private val ViewHolder.messageListItem: MessageListItem?
        get() = when (this) {
            is MessageViewHolder -> adapter.getItemById(uniqueId)
            is ComposableMessageViewHolder -> adapter.getItemById(uniqueId)
            else -> null
        }
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
    val icon: Drawable?,
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
