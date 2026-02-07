package com.fsck.k9.ui.settings.notificationactions

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.ui.R
import com.fsck.k9.view.DraggableFrameLayout
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

internal sealed interface NotificationListItem {
    data class Action(
        val action: MessageNotificationAction,
        val isDimmed: Boolean,
    ) : NotificationListItem

    object Cutoff : NotificationListItem
}

internal class NotificationActionsAdapter(
    private val themeProvider: FeatureThemeProvider,
    private val onDragFinished: (List<NotificationListItem>) -> Unit,
) : RecyclerView.Adapter<NotificationActionsAdapter.BaseViewHolder>() {
    private val items = mutableListOf<NotificationListItem>()
    private var dragDidMove = false

    private val itemTouchHelper = ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0,
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                val moved = moveItem(from, to)
                if (moved) {
                    dragDidMove = true
                }
                return moved
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    (viewHolder?.itemView as? DraggableFrameLayout)?.isDragged = true
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                (viewHolder.itemView as? DraggableFrameLayout)?.isDragged = false
                if (dragDidMove) {
                    dragDidMove = false
                    onDragFinished(items.toList())
                }
            }
        },
    )

    init {
        setHasStableIds(true)
    }

    fun setItems(newItems: List<NotificationListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun attachTo(recyclerView: RecyclerView) {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return when (val item = items[position]) {
            is NotificationListItem.Action -> 1000L + item.action.ordinal
            is NotificationListItem.Cutoff -> 2000L
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is NotificationListItem.Action -> VIEW_TYPE_ACTION
        is NotificationListItem.Cutoff -> VIEW_TYPE_CUTOFF
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val context = parent.context
        return when (viewType) {
            VIEW_TYPE_ACTION -> ActionViewHolder(
                container = createDraggableContainer(context),
                themeProvider = themeProvider,
            )
            VIEW_TYPE_CUTOFF -> CutoffViewHolder(
                container = createDraggableContainer(
                    context = context,
                    contentDescriptionRes = R.string.notification_actions_cutoff_description,
                ),
                themeProvider = themeProvider,
            )
            else -> error("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (val item = items[position]) {
            is NotificationListItem.Action -> {
                val actionHolder = holder as ActionViewHolder
                actionHolder.bind(
                    item = item,
                    onStartDrag = { itemTouchHelper.startDrag(actionHolder) },
                    onMove = ::moveItem,
                )
            }
            is NotificationListItem.Cutoff -> {
                val cutoffHolder = holder as CutoffViewHolder
                cutoffHolder.bind(
                    onStartDrag = { itemTouchHelper.startDrag(cutoffHolder) },
                    onMove = ::moveItem,
                )
            }
        }
    }

    private fun moveItem(from: Int, to: Int): Boolean {
        val lastIndex = items.lastIndex
        if (from == RecyclerView.NO_POSITION || to !in 0..lastIndex) return false
        val item = items.getOrNull(from) ?: return false
        val targetPosition = if (item is NotificationListItem.Cutoff) {
            to.coerceAtMost(NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN).coerceIn(0, lastIndex)
        } else {
            to
        }
        if (targetPosition == from) return false
        items.add(targetPosition, items.removeAt(from))
        notifyItemMoved(from, targetPosition)
        return true
    }

    internal sealed class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view)

    internal class ActionViewHolder(
        private val container: DraggableFrameLayout,
        private val themeProvider: FeatureThemeProvider,
    ) : BaseViewHolder(container) {
        private val composeView = ComposeView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }

        init {
            container.addView(composeView)
        }

        fun bind(
            item: NotificationListItem.Action,
            onStartDrag: () -> Unit,
            onMove: (Int, Int) -> Boolean,
        ) {
            setReorderActions(onMove)
            composeView.setContent {
                themeProvider.WithTheme {
                    NotificationActionRow(
                        action = item.action,
                        isDimmed = item.isDimmed,
                        onStartDrag = onStartDrag,
                    )
                }
            }
        }

        private fun setReorderActions(onMove: (Int, Int) -> Boolean) {
            val context = itemView.context
            val moveUpLabel = context.getString(R.string.accessibility_move_up)
            val moveDownLabel = context.getString(R.string.accessibility_move_down)
            ViewCompat.replaceAccessibilityAction(
                itemView,
                AccessibilityActionCompat(
                    R.id.accessibility_action_move_up,
                    moveUpLabel,
                ),
                moveUpLabel,
            ) { _, _ ->
                val from = bindingAdapterPosition
                onMove(from, from - 1)
            }
            ViewCompat.replaceAccessibilityAction(
                itemView,
                AccessibilityActionCompat(
                    R.id.accessibility_action_move_down,
                    moveDownLabel,
                ),
                moveDownLabel,
            ) { _, _ ->
                val from = bindingAdapterPosition
                onMove(from, from + 1)
            }
        }
    }

    internal class CutoffViewHolder(
        private val container: DraggableFrameLayout,
        private val themeProvider: FeatureThemeProvider,
    ) : BaseViewHolder(container) {
        private val composeView = ComposeView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }

        init {
            container.addView(composeView)
        }

        fun bind(
            onStartDrag: () -> Unit,
            onMove: (Int, Int) -> Boolean,
        ) {
            setReorderActions(onMove)
            composeView.setContent {
                themeProvider.WithTheme {
                    NotificationCutoffRow(
                        onStartDrag = onStartDrag,
                    )
                }
            }
        }

        private fun setReorderActions(onMove: (Int, Int) -> Boolean) {
            val context = itemView.context
            val moveUpLabel = context.getString(R.string.accessibility_move_up)
            val moveDownLabel = context.getString(R.string.accessibility_move_down)
            ViewCompat.replaceAccessibilityAction(
                itemView,
                AccessibilityActionCompat(R.id.accessibility_action_move_up, moveUpLabel),
                moveUpLabel,
            ) { _, _ ->
                val from = bindingAdapterPosition
                onMove(from, from - 1)
            }
            ViewCompat.replaceAccessibilityAction(
                itemView,
                AccessibilityActionCompat(R.id.accessibility_action_move_down, moveDownLabel),
                moveDownLabel,
            ) { _, _ ->
                val from = bindingAdapterPosition
                onMove(from, from + 1)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_ACTION = 0
        private const val VIEW_TYPE_CUTOFF = 1
    }
}

@Composable
private fun NotificationActionRow(
    action: MessageNotificationAction,
    isDimmed: Boolean,
    onStartDrag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minHeight = MainTheme.sizes.iconAvatar
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .alpha(if (isDimmed) 0.6f else 1.0f)
            .padding(start = MainTheme.spacings.default, end = MainTheme.spacings.zero)
            .pointerInteropFilter { event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onStartDrag()
                    true
                } else {
                    false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(action.iconRes),
            contentDescription = null,
            modifier = Modifier.padding(MainTheme.spacings.default),
        )
        TextBodyLarge(
            text = stringResource(action.labelRes),
            modifier = Modifier
                .weight(1f)
                .padding(start = MainTheme.spacings.triple, end = MainTheme.spacings.double),
        )
        Image(
            painter = painterResource(Icons.Outlined.DragHandle),
            contentDescription = stringResource(R.string.notification_actions_drag_handle_description),
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

@Composable
private fun NotificationCutoffRow(
    onStartDrag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minHeight = MainTheme.sizes.iconAvatar
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(start = MainTheme.spacings.double, end = MainTheme.spacings.zero)
            .pointerInteropFilter { event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onStartDrag()
                    true
                } else {
                    false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = MainTheme.spacings.double),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MainTheme.spacings.quarter)
                    .align(Alignment.Center)
                    .alpha(0.6f)
                    .background(MainTheme.colors.primary),
            )
        }
    }
}

private fun createDraggableContainer(
    context: Context,
    @StringRes contentDescriptionRes: Int? = null,
): DraggableFrameLayout {
    val container = DraggableFrameLayout(context).apply {
        layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        background = resolveDrawable(context, android.R.attr.windowBackground)
        foreground = resolveDrawable(context, android.R.attr.selectableItemBackground)
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.draggable_state_list_anim)
        isClickable = true
        isFocusable = true
        if (contentDescriptionRes != null) {
            contentDescription = context.getString(contentDescriptionRes)
        }
    }
    return container
}

private fun resolveDrawable(context: Context, attrRes: Int) =
    TypedValue().let { value ->
        if (!context.theme.resolveAttribute(attrRes, value, true)) return@let null
        if (value.resourceId != 0) {
            return@let ResourcesCompat.getDrawable(context.resources, value.resourceId, context.theme)
        }
        if (value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) {
            return@let ColorDrawable(value.data)
        }
        null
    }
