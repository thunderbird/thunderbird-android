package com.fsck.k9.ui.settings.notificationactions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.ui.R
import kotlinx.collections.immutable.ImmutableList

private const val ARROW_DISABLED_ALPHA = 0.38f
private const val DIMMED_ROW_ALPHA = 0.6f
private const val DRAGGED_ROW_SCALE = 1.02f
private const val DRAGGED_ROW_ELEVATION_DP = 12

@Composable
internal fun NotificationActionsSettingsScreen(
    description: String,
    initialActions: ImmutableList<MessageNotificationAction>,
    initialCutoff: Int,
    onStateChanged: (List<MessageNotificationAction>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reorderController = rememberReorderController(
        initialActions = initialActions,
        initialCutoff = initialCutoff,
        onStateChanged = onStateChanged,
    )
    NotificationActionsContent(
        description = description,
        reorderController = reorderController,
        modifier = modifier,
    )
}

@Composable
private fun rememberReorderController(
    initialActions: ImmutableList<MessageNotificationAction>,
    initialCutoff: Int,
    onStateChanged: (List<MessageNotificationAction>, Int) -> Unit,
): NotificationActionsReorderController {
    val reorderController = remember {
        NotificationActionsReorderController(
            initialActions = initialActions,
            initialCutoff = initialCutoff,
            onStateChanged = onStateChanged,
        )
    }

    LaunchedEffect(initialActions, initialCutoff) {
        reorderController.setState(
            initialActions = initialActions,
            initialCutoff = initialCutoff,
        )
    }

    return reorderController
}

@Composable
private fun NotificationActionsContent(
    description: String,
    reorderController: NotificationActionsReorderController,
    modifier: Modifier = Modifier,
) {
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

            NotificationActionsList(reorderController = reorderController)
        }
    }
}

@Composable
private fun NotificationActionsList(reorderController: NotificationActionsReorderController) {
    val listState = rememberLazyListState()
    val onDrag: (Float) -> Unit = { deltaY ->
        reorderController.dragBy(
            deltaY = deltaY,
            visibleItems = listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                val key = info.key as? String ?: return@mapNotNull null
                ReorderVisibleItem(
                    key = key,
                    offset = info.offset,
                    size = info.size,
                )
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            vertical = MainTheme.spacings.default,
        ),
    ) {
        itemsIndexed(
            items = reorderController.items,
            key = { _, item -> item.key },
        ) { index, item ->
            val isDragged = reorderController.draggedKey == item.key
            val rowModifier = if (isDragged) Modifier else Modifier.animateItem()
            NotificationActionsListItem(
                item = item,
                index = index,
                reorderController = reorderController,
                onDrag = onDrag,
                rowModifier = rowModifier,
            )
        }
    }
}

@Composable
private fun NotificationActionsListItem(
    item: NotificationListItem,
    index: Int,
    reorderController: NotificationActionsReorderController,
    onDrag: (Float) -> Unit,
    rowModifier: Modifier = Modifier,
) {
    val isDragged = reorderController.draggedKey == item.key
    val dragState = RowDragState(
        isDragged = isDragged,
        draggedOffsetY = reorderController.draggedOffsetY,
    )
    val moveActions = MoveActions(
        moveUpLabel = stringResource(R.string.accessibility_move_up),
        moveDownLabel = stringResource(R.string.accessibility_move_down),
        onMoveUp = { reorderController.moveByStep(item.key, -1) },
        onMoveDown = { reorderController.moveByStep(item.key, 1) },
    )
    val dragCallbacks = DragCallbacks(
        onDragStart = { reorderController.startDrag(item.key) },
        onDrag = onDrag,
        onDragEnd = reorderController::endDrag,
    )
    when (item) {
        is NotificationListItem.Action -> NotificationActionRow(
            action = item.action,
            rowState = ActionRowState(
                isDimmed = index > reorderController.cutoffIndex,
                canMoveUp = reorderController.canMove(item.key, -1),
                canMoveDown = reorderController.canMove(item.key, 1),
                dragState = dragState,
            ),
            moveActions = moveActions,
            dragCallbacks = dragCallbacks,
            modifier = rowModifier,
        )

        is NotificationListItem.Cutoff -> NotificationCutoffRow(
            dragState = dragState,
            moveActions = moveActions,
            dragCallbacks = dragCallbacks,
            modifier = rowModifier,
        )
    }
}

private data class ActionRowState(
    val isDimmed: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val dragState: RowDragState,
)

@Composable
private fun NotificationActionRow(
    action: MessageNotificationAction,
    rowState: ActionRowState,
    moveActions: MoveActions,
    dragCallbacks: DragCallbacks,
    modifier: Modifier = Modifier,
) {
    val contentLabel = stringResource(action.labelRes)
    val dragState = rowState.dragState.copy(
        alpha = if (rowState.dragState.isDragged || !rowState.isDimmed) 1.0f else DIMMED_ROW_ALPHA,
    )

    NotificationReorderRow(
        contentDescription = contentLabel,
        moveActions = moveActions,
        dragState = dragState,
        startPadding = MainTheme.spacings.default,
        dragCallbacks = dragCallbacks,
        modifier = modifier
            .heightIn(min = MainTheme.sizes.iconAvatar),
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
            modifier = Modifier.padding(end = MainTheme.spacings.double),
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            ArrowButton(
                iconRes = Icons.Outlined.ExpandLess,
                contentDescription = moveActions.moveUpLabel,
                enabled = rowState.canMoveUp,
                onClick = moveActions.onMoveUp,
            )
            ArrowButton(
                iconRes = Icons.Outlined.ExpandMore,
                contentDescription = moveActions.moveDownLabel,
                enabled = rowState.canMoveDown,
                onClick = moveActions.onMoveDown,
            )
        }
    }
}

@Composable
private fun NotificationCutoffRow(
    dragState: RowDragState,
    moveActions: MoveActions,
    dragCallbacks: DragCallbacks,
    modifier: Modifier = Modifier,
) {
    val cutoffContentLabel = stringResource(R.string.notification_actions_cutoff_description)

    NotificationReorderRow(
        contentDescription = cutoffContentLabel,
        moveActions = moveActions,
        dragState = dragState.copy(alpha = 1f),
        startPadding = MainTheme.spacings.double,
        dragCallbacks = dragCallbacks,
        modifier = modifier
            .heightIn(min = MainTheme.sizes.iconAvatar),
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
                    .alpha(DIMMED_ROW_ALPHA)
                    .background(MainTheme.colors.primary),
            )
        }
    }
}

private data class RowDragState(
    val isDragged: Boolean,
    val draggedOffsetY: Float,
    val alpha: Float = 1f,
)

private data class DragCallbacks(
    val onDragStart: () -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit,
)

private data class MoveActions(
    val moveUpLabel: String,
    val moveDownLabel: String,
    val onMoveUp: () -> Boolean,
    val onMoveDown: () -> Boolean,
)

@Composable
private fun NotificationReorderRow(
    contentDescription: String,
    moveActions: MoveActions,
    dragState: RowDragState,
    startPadding: androidx.compose.ui.unit.Dp,
    dragCallbacks: DragCallbacks,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val dragScale by animateFloatAsState(
        targetValue = if (dragState.isDragged) DRAGGED_ROW_SCALE else 1.0f,
        label = "notificationRowDragScale",
    )
    val density = LocalDensity.current
    val dragElevationPx = with(density) { if (dragState.isDragged) DRAGGED_ROW_ELEVATION_DP.dp.toPx() else 0f }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(dragState.alpha)
            .graphicsLayer {
                translationY = if (dragState.isDragged) dragState.draggedOffsetY else 0f
                scaleX = dragScale
                scaleY = dragScale
                shadowElevation = dragElevationPx
            }
            .zIndex(if (dragState.isDragged) 1f else 0f)
            .background(MainTheme.colors.surface)
            .padding(start = startPadding, end = MainTheme.spacings.zero)
            .immediateDragGesture(
                onDragStart = dragCallbacks.onDragStart,
                onDrag = dragCallbacks.onDrag,
                onDragEnd = dragCallbacks.onDragEnd,
            )
            .semantics {
                this.contentDescription = contentDescription
                customActions = listOf(
                    CustomAccessibilityAction(moveActions.moveUpLabel) { moveActions.onMoveUp() },
                    CustomAccessibilityAction(moveActions.moveDownLabel) { moveActions.onMoveDown() },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
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
            .padding(vertical = MainTheme.spacings.default)
            .alpha(if (enabled) 1f else ARROW_DISABLED_ALPHA)
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
            onDrag = { _, dragAmount ->
                if (dragAmount.y != 0f) {
                    onDrag(dragAmount.y)
                }
            },
        )
    }
}
