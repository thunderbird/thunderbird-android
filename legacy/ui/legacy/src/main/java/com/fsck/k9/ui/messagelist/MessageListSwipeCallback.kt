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
import com.fsck.k9.ui.messagelist.item.MessageViewHolder
import com.google.android.material.color.ColorRoles
import com.google.android.material.textview.MaterialTextView
import kotlin.math.abs
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountWrapper
import net.thunderbird.core.architecture.data.DataMapper
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.feature.mail.message.list.domain.DomainContract

@SuppressLint("InflateParams")
@Suppress("LongParameterList")
class MessageListSwipeCallback(
    context: Context,
    private val resourceProvider: SwipeResourceProvider,
    private val swipeActionSupportProvider: SwipeActionSupportProvider,
    private val buildSwipeActions: DomainContract.UseCase.BuildSwipeActions<LegacyAccount>,
    private val adapter: MessageListAdapter,
    private val listener: MessageListSwipeListener,
    accounts: List<LegacyAccountWrapper>,
    private val legacyAccountWrapperDataMapper: DataMapper<LegacyAccountWrapper, LegacyAccount>,
) : ItemTouchHelper.Callback() {
    private var swipeActions: Map<String, SwipeActions> = emptyMap()
    private val swipePadding = context.resources.getDimension(R.dimen.messageListSwipeIconPadding).toInt()
    private val swipeThreshold = context.resources.getDimension(R.dimen.messageListSwipeThreshold)
    private val backgroundColorPaint = Paint()

    private val swipeRightLayout: View
    private val swipeRightIcon: ImageView
    private val swipeRightText: MaterialTextView
    private val swipeLeftLayout: View
    private val swipeLeftIcon: ImageView
    private val swipeLeftText: MaterialTextView
    private val swipeRightConfig: MutableMap<String, SwipeActionConfig> = mutableMapOf()
    private val swipeLeftConfig: MutableMap<String, SwipeActionConfig> = mutableMapOf()

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

        invalidateSwipeActions(accounts)
    }

    override fun isFlingEnabled(): Boolean {
        return false
    }

    private val MessageListItem.swipeActions: SwipeActions
        get() {
            val swipeActions = this@MessageListSwipeCallback.swipeActions
            return requireNotNull(swipeActions[account.uuid]) {
                "Could not find swipe actions for account ${account.uuid}. swipeActions = $swipeActions"
            }
        }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        if (viewHolder !is MessageViewHolder) return 0

        val item = adapter.getItemById(viewHolder.uniqueId) ?: return 0
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
                val holder = viewHolder as MessageViewHolder
                val item = adapter.getItemById(holder.uniqueId) ?: return@withTranslation
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
        if (swipeActionConfig[item.accountWrapper.uuid] == null) {
            error("drawBackground() called despite swipeActionConfig[item.accountWrapper] == null")
        }

        backgroundColorPaint.color = swipeActionConfig.getValue(item.accountWrapper.uuid).backgroundColor
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
        val account = item.accountWrapper

        val swipeActionConfig = if (swipeRight) swipeRightConfig[account.uuid] else swipeLeftConfig[account.uuid]
        if (swipeActionConfig == null) {
            error("drawLayout() called despite swipeActionConfig == null")
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
            ?.let { swipeActions[it.account.uuid] } ?: return false

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

    fun invalidateSwipeActions(accounts: List<LegacyAccountWrapper>) {
        swipeActions = buildSwipeActions(accounts)
        swipeLeftConfig.apply {
            clear()
            putAll(setupSwipeActionConfig(accounts = accounts, resourceProvider = resourceProvider, isLeft = true))
        }
        swipeRightConfig.apply {
            clear()
            putAll(setupSwipeActionConfig(accounts = accounts, resourceProvider = resourceProvider, isLeft = false))
        }
    }

    fun buildSwipeActions(accounts: List<LegacyAccountWrapper>): Map<String, SwipeActions> {
        return buildSwipeActions(
            accountUuids = accounts.map { it.uuid }.toSet(),
            isIncomingServerPop3 = { account ->
                legacyAccountWrapperDataMapper.toDomain(account).isIncomingServerPop3()
            },
            hasArchiveFolder = { account -> legacyAccountWrapperDataMapper.toDomain(account).hasArchiveFolder() },
        )
    }

    private fun setupSwipeActionConfig(
        accounts: List<LegacyAccountWrapper>,
        resourceProvider: SwipeResourceProvider,
        isLeft: Boolean,
    ): Map<String, SwipeActionConfig> {
        return accounts
            .mapNotNull { account ->
                swipeActions[account.uuid]
                    ?.let { (swipeActionLeft, swipeActionRight) ->
                        if (isLeft) swipeActionLeft else swipeActionRight
                    }
                    ?.takeIf { swipeAction -> swipeAction != SwipeAction.None }
                    ?.let { swipeAction -> account.uuid to swipeAction }
            }
            .associate { (uuid, swipeAction) ->
                uuid to SwipeActionConfig(
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
        get() = (this as? MessageViewHolder)?.uniqueId?.let { adapter.getItemById(it) }

    private val MessageListItem.accountWrapper: LegacyAccountWrapper
        get() = legacyAccountWrapperDataMapper.toDomain(account)
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
