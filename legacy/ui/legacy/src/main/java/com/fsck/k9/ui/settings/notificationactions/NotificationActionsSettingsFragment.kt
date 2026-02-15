package com.fsck.k9.ui.settings.notificationactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.ui.R
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
import net.thunderbird.core.preference.update
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject

/**
 * Lets users reorder notification actions and position the cutoff line.
 */
class NotificationActionsSettingsFragment : androidx.fragment.app.Fragment() {
    private val generalSettingsManager: GeneralSettingsManager by inject()
    private val themeProvider: FeatureThemeProvider by inject()

    private var actionOrder: MutableList<MessageNotificationAction> = MessageNotificationAction
        .defaultOrder()
        .toMutableList()
    private var cutoff: Int = NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        initializeStateFromPreferences()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme {
                    NotificationActionsSettingsScreen(
                        description = stringResource(R.string.notification_actions_settings_description),
                        initialItems = buildItems(),
                        onItemsChanged = ::onDragFinished,
                    )
                }
            }
        }
    }

    private fun initializeStateFromPreferences() {
        val notificationPrefs = generalSettingsManager.getConfig().notification

        actionOrder = parseOrder(notificationPrefs.messageActionsOrder).toMutableList()
        cutoff = notificationPrefs.messageActionsCutoff.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
    }

    private fun persist() {
        val sanitizedCutoff = cutoff.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
        generalSettingsManager.update { settings ->
            settings.copy(
                notification = settings.notification.copy(
                    messageActionsOrder = actionOrder.map { it.token },
                    messageActionsCutoff = sanitizedCutoff,
                ),
            )
        }
    }

    private fun buildItems(): List<NotificationListItem> {
        val clampedCutoff =
            cutoff.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN).coerceAtMost(actionOrder.size)
        return buildList {
            actionOrder.forEachIndexed { index, action ->
                if (index == clampedCutoff) add(NotificationListItem.Cutoff)
                add(
                    NotificationListItem.Action(
                        action = action,
                        isDimmed = false,
                    ),
                )
            }
            if (clampedCutoff == actionOrder.size) add(NotificationListItem.Cutoff)
        }
    }

    private fun onDragFinished(items: List<NotificationListItem>) {
        actionOrder = items
            .filterIsInstance<NotificationListItem.Action>()
            .map { it.action }
            .toMutableList()
        cutoff = items.indexOfFirst { it is NotificationListItem.Cutoff }
            .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
        persist()
    }

    private fun parseOrder(tokens: List<String>): List<MessageNotificationAction> {
        val seen = LinkedHashSet<MessageNotificationAction>()
        for (token in tokens) {
            MessageNotificationAction.fromToken(token)?.let { seen.add(it) }
        }

        for (action in MessageNotificationAction.defaultOrder()) {
            seen.add(action)
        }

        return seen.toList()
    }

}

@Composable
private fun NotificationActionsSettingsScreen(
    description: String,
    initialItems: List<NotificationListItem>,
    onItemsChanged: (List<NotificationListItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember { mutableStateListOf<NotificationListItem>() }
    val listState = rememberLazyListState()
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var draggedOffsetY by remember { mutableFloatStateOf(0f) }
    var dragDidMove by remember { mutableStateOf(false) }

    LaunchedEffect(initialItems) {
        items.clear()
        items.addAll(normalizeItems(initialItems))
    }

    fun startDrag(itemKey: String) {
        draggedKey = itemKey
        draggedOffsetY = 0f
        dragDidMove = false
    }

    fun moveByAccessibility(itemKey: String, delta: Int): Boolean {
        val from = items.indexOfFirst { it.key == itemKey }
        if (from == -1) return false

        val moved = moveItem(items, from, from + delta)
        if (moved) {
            onItemsChanged(items.toList())
        }

        return moved
    }

    fun onDrag(deltaY: Float) {
        val key = draggedKey ?: return
        draggedOffsetY += deltaY

        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val draggedItem = visibleItems.firstOrNull { it.key == key } ?: return
        val fromIndex = draggedItem.index

        if (deltaY > 0f) {
            val nextItem = visibleItems
                .filter { it.index > fromIndex }
                .minByOrNull { it.index } ?: return

            val draggedBottom = draggedItem.offset + draggedItem.size + draggedOffsetY
            val nextThreshold = nextItem.offset + (nextItem.size / 2f)
            if (draggedBottom >= nextThreshold) {
                val moved = moveItem(items, fromIndex, nextItem.index)
                if (moved) {
                    dragDidMove = true
                    draggedOffsetY -= nextItem.size.toFloat()
                }
            }
        } else if (deltaY < 0f) {
            val previousItem = visibleItems
                .filter { it.index < fromIndex }
                .maxByOrNull { it.index } ?: return

            val draggedTop = draggedItem.offset + draggedOffsetY
            val previousThreshold = previousItem.offset + (previousItem.size / 2f)
            if (draggedTop <= previousThreshold) {
                val moved = moveItem(items, fromIndex, previousItem.index)
                if (moved) {
                    dragDidMove = true
                    draggedOffsetY += previousItem.size.toFloat()
                }
            }
        }
    }

    fun endDrag() {
        draggedOffsetY = 0f
        draggedKey = null

        if (dragDidMove) {
            dragDidMove = false
            onItemsChanged(items.toList())
        }
    }

    val cutoffIndex = items.indexOfFirst { it is NotificationListItem.Cutoff }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TextBodyMedium(
                text = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MainTheme.spacings.double,
                        end = MainTheme.spacings.double,
                        top = MainTheme.spacings.double,
                        bottom = MainTheme.spacings.default,
                    ),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = MainTheme.spacings.default),
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.key },
                ) { index, item ->
                    val isDragged = draggedKey == item.key
                    when (item) {
                        is NotificationListItem.Action -> NotificationActionRow(
                            action = item.action,
                            isDimmed = index > cutoffIndex,
                            onDragStart = { startDrag(item.key) },
                            onDrag = ::onDrag,
                            onDragEnd = ::endDrag,
                            onMoveUp = { moveByAccessibility(item.key, -1) },
                            onMoveDown = { moveByAccessibility(item.key, 1) },
                            canMoveUp = canMove(items, index, -1),
                            canMoveDown = canMove(items, index, 1),
                            isDragged = isDragged,
                            draggedOffsetY = draggedOffsetY,
                            modifier = if (isDragged) Modifier else Modifier.animateItem(),
                        )

                        is NotificationListItem.Cutoff -> NotificationCutoffRow(
                            onDragStart = { startDrag(item.key) },
                            onDrag = ::onDrag,
                            onDragEnd = ::endDrag,
                            onMoveUp = { moveByAccessibility(item.key, -1) },
                            onMoveDown = { moveByAccessibility(item.key, 1) },
                            canMoveUp = canMove(items, index, -1),
                            canMoveDown = canMove(items, index, 1),
                            isDragged = isDragged,
                            draggedOffsetY = draggedOffsetY,
                            modifier = if (isDragged) Modifier else Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationActionRow(
    action: MessageNotificationAction,
    isDimmed: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isDragged: Boolean,
    draggedOffsetY: Float,
    modifier: Modifier = Modifier,
) {
    val minHeight = MainTheme.sizes.iconAvatar
    val moveUpLabel = stringResource(R.string.accessibility_move_up)
    val moveDownLabel = stringResource(R.string.accessibility_move_down)
    val contentLabel = stringResource(action.labelRes)
    val dragScale by animateFloatAsState(
        targetValue = if (isDragged) 1.02f else 1.0f,
        label = "notificationActionDragScale",
    )
    val density = LocalDensity.current
    val dragElevationPx = with(density) { if (isDragged) 12.dp.toPx() else 0f }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .alpha(if (isDimmed) 0.6f else 1.0f)
            .graphicsLayer {
                translationY = if (isDragged) draggedOffsetY else 0f
                scaleX = dragScale
                scaleY = dragScale
                shadowElevation = dragElevationPx
            }
            .zIndex(if (isDragged) 1f else 0f)
            .background(MainTheme.colors.surface)
            .padding(start = MainTheme.spacings.default, end = MainTheme.spacings.zero)
            .immediateDragGesture(
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
            .semantics {
                contentDescription = contentLabel
                customActions = listOf(
                    CustomAccessibilityAction(moveUpLabel) { onMoveUp() },
                    CustomAccessibilityAction(moveDownLabel) { onMoveDown() },
                )
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
        Row(
            modifier = Modifier.padding(end = MainTheme.spacings.default),
        ) {
            ArrowButton(
                iconRes = Icons.Outlined.ExpandLess,
                contentDescription = moveUpLabel,
                enabled = canMoveUp,
                onClick = onMoveUp,
            )
            ArrowButton(
                iconRes = Icons.Outlined.ExpandMore,
                contentDescription = moveDownLabel,
                enabled = canMoveDown,
                onClick = onMoveDown,
            )
        }
    }
}

@Composable
private fun NotificationCutoffRow(
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isDragged: Boolean,
    draggedOffsetY: Float,
    modifier: Modifier = Modifier,
) {
    val minHeight = MainTheme.sizes.iconAvatar
    val moveUpLabel = stringResource(R.string.accessibility_move_up)
    val moveDownLabel = stringResource(R.string.accessibility_move_down)
    val cutoffContentLabel = stringResource(R.string.notification_actions_cutoff_description)
    val dragScale by animateFloatAsState(
        targetValue = if (isDragged) 1.02f else 1.0f,
        label = "notificationCutoffDragScale",
    )
    val density = LocalDensity.current
    val dragElevationPx = with(density) { if (isDragged) 12.dp.toPx() else 0f }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .graphicsLayer {
                translationY = if (isDragged) draggedOffsetY else 0f
                scaleX = dragScale
                scaleY = dragScale
                shadowElevation = dragElevationPx
            }
            .zIndex(if (isDragged) 1f else 0f)
            .background(MainTheme.colors.surface)
            .padding(start = MainTheme.spacings.double, end = MainTheme.spacings.zero)
            .immediateDragGesture(
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
            .semantics {
                contentDescription = cutoffContentLabel
                customActions = listOf(
                    CustomAccessibilityAction(moveUpLabel) { onMoveUp() },
                    CustomAccessibilityAction(moveDownLabel) { onMoveDown() },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = MainTheme.spacings.default),
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
        Row(
            modifier = Modifier.padding(end = MainTheme.spacings.default),
        ) {
            ArrowButton(
                iconRes = Icons.Outlined.ExpandLess,
                contentDescription = moveUpLabel,
                enabled = canMoveUp,
                onClick = onMoveUp,
            )
            ArrowButton(
                iconRes = Icons.Outlined.ExpandMore,
                contentDescription = moveDownLabel,
                enabled = canMoveDown,
                onClick = onMoveDown,
            )
        }
    }
}

@Composable
private fun ArrowButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = modifier
            .padding(MainTheme.spacings.half)
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled) { onClick() },
    )
}

private fun Modifier.immediateDragGesture(
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
): Modifier {
    return pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { onDragStart() },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() },
            onDrag = { change, dragAmount ->
                if (dragAmount.y != 0f) {
                    onDrag(dragAmount.y)
                }
            },
        )
    }
}

private fun normalizeItems(items: List<NotificationListItem>): List<NotificationListItem> {
    val actions = items.filterIsInstance<NotificationListItem.Action>().map { it.action }
    val cutoff = items.indexOfFirst { it is NotificationListItem.Cutoff }
        .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
        .coerceAtMost(actions.size)

    return normalizeItems(actions = actions, cutoff = cutoff)
}

private fun normalizeItems(
    actions: List<MessageNotificationAction>,
    cutoff: Int,
): List<NotificationListItem> {
    val clampedCutoff = cutoff
        .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
        .coerceAtMost(actions.size)
    return buildList {
        actions.forEachIndexed { index, action ->
            if (index == clampedCutoff) add(NotificationListItem.Cutoff)
            add(NotificationListItem.Action(action = action, isDimmed = false))
        }
        if (clampedCutoff == actions.size) add(NotificationListItem.Cutoff)
    }
}

private fun moveItem(
    items: MutableList<NotificationListItem>,
    from: Int,
    to: Int,
): Boolean {
    val lastIndex = items.lastIndex
    if (from !in 0..lastIndex || to !in 0..lastIndex) return false

    val item = items[from]
    val oldCutoff = items.indexOfFirst { it is NotificationListItem.Cutoff }
        .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
    val boundedTarget = if (item is NotificationListItem.Cutoff) {
        to.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN.coerceAtMost(lastIndex))
    } else {
        val targetItem = items.getOrNull(to)
        when {
            targetItem is NotificationListItem.Cutoff && to < from -> (to - 1).coerceAtLeast(0)
            targetItem is NotificationListItem.Cutoff && to > from -> (to + 1).coerceAtMost(lastIndex)
            else -> to
        }
    }

    if (boundedTarget == from) return false

    val normalized = if (item is NotificationListItem.Cutoff) {
        val actions = items.filterIsInstance<NotificationListItem.Action>().map { it.action }
        normalizeItems(actions = actions, cutoff = boundedTarget)
    } else {
        val mutable = items.toMutableList()
        mutable.add(boundedTarget, mutable.removeAt(from))
        val reorderedActions = mutable.filterIsInstance<NotificationListItem.Action>().map { it.action }
        normalizeItems(actions = reorderedActions, cutoff = oldCutoff)
    }

    items.clear()
    items.addAll(normalized)
    return true
}

private fun canMove(
    items: List<NotificationListItem>,
    from: Int,
    delta: Int,
): Boolean {
    val lastIndex = items.lastIndex
    val to = from + delta
    if (from !in 0..lastIndex || to !in 0..lastIndex) return false

    val item = items[from]
    val boundedTarget = if (item is NotificationListItem.Cutoff) {
        to.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN.coerceAtMost(lastIndex))
    } else {
        val targetItem = items.getOrNull(to)
        when {
            targetItem is NotificationListItem.Cutoff && to < from -> (to - 1).coerceAtLeast(0)
            targetItem is NotificationListItem.Cutoff && to > from -> (to + 1).coerceAtMost(lastIndex)
            else -> to
        }
    }

    return boundedTarget != from
}

private val NotificationListItem.key: String
    get() = when (this) {
        is NotificationListItem.Action -> "action:${action.token}"
        NotificationListItem.Cutoff -> "cutoff"
    }
