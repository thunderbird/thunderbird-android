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

private const val ARROW_DISABLED_ALPHA = 0.38f

@Composable
internal fun NotificationActionsSettingsScreen(
    description: String,
    initialActions: List<MessageNotificationAction>,
    initialCutoff: Int,
    onStateChanged: (List<MessageNotificationAction>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
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
                    items = reorderController.items,
                    key = { _, item -> item.key },
                ) { index, item ->
                    val isDragged = reorderController.draggedKey == item.key
                    val rowModifier = if (isDragged) Modifier else Modifier.animateItem()
                    when (item) {
                        is NotificationListItem.Action -> NotificationActionRow(
                            action = item.action,
                            isDimmed = index > reorderController.cutoffIndex,
                            onDragStart = { reorderController.startDrag(item.key) },
                            onDrag = onDrag,
                            onDragEnd = reorderController::endDrag,
                            onMoveUp = { reorderController.moveByStep(item.key, -1) },
                            onMoveDown = { reorderController.moveByStep(item.key, 1) },
                            canMoveUp = reorderController.canMove(item.key, -1),
                            canMoveDown = reorderController.canMove(item.key, 1),
                            isDragged = isDragged,
                            draggedOffsetY = reorderController.draggedOffsetY,
                            modifier = rowModifier,
                        )

                        is NotificationListItem.Cutoff -> NotificationCutoffRow(
                            onDragStart = { reorderController.startDrag(item.key) },
                            onDrag = onDrag,
                            onDragEnd = reorderController::endDrag,
                            onMoveUp = { reorderController.moveByStep(item.key, -1) },
                            onMoveDown = { reorderController.moveByStep(item.key, 1) },
                            isDragged = isDragged,
                            draggedOffsetY = reorderController.draggedOffsetY,
                            modifier = rowModifier,
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

    NotificationReorderRow(
        contentDescription = contentLabel,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        moveUpLabel = moveUpLabel,
        moveDownLabel = moveDownLabel,
        isDragged = isDragged,
        draggedOffsetY = draggedOffsetY,
        alpha = if (isDragged) 1.0f else if (isDimmed) 0.6f else 1.0f,
        startPadding = MainTheme.spacings.default,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        modifier = modifier
            .heightIn(min = minHeight),
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
    isDragged: Boolean,
    draggedOffsetY: Float,
    modifier: Modifier = Modifier,
) {
    val minHeight = MainTheme.sizes.iconAvatar
    val moveUpLabel = stringResource(R.string.accessibility_move_up)
    val moveDownLabel = stringResource(R.string.accessibility_move_down)
    val cutoffContentLabel = stringResource(R.string.notification_actions_cutoff_description)

    NotificationReorderRow(
        contentDescription = cutoffContentLabel,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        moveUpLabel = moveUpLabel,
        moveDownLabel = moveDownLabel,
        isDragged = isDragged,
        draggedOffsetY = draggedOffsetY,
        alpha = 1f,
        startPadding = MainTheme.spacings.double,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        modifier = modifier
            .heightIn(min = minHeight),
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

@Composable
private fun NotificationReorderRow(
    contentDescription: String,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    moveUpLabel: String,
    moveDownLabel: String,
    isDragged: Boolean,
    draggedOffsetY: Float,
    alpha: Float,
    startPadding: androidx.compose.ui.unit.Dp,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val dragScale by animateFloatAsState(
        targetValue = if (isDragged) 1.02f else 1.0f,
        label = "notificationRowDragScale",
    )
    val density = LocalDensity.current
    val dragElevationPx = with(density) { if (isDragged) 12.dp.toPx() else 0f }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .graphicsLayer {
                translationY = if (isDragged) draggedOffsetY else 0f
                scaleX = dragScale
                scaleY = dragScale
                shadowElevation = dragElevationPx
            }
            .zIndex(if (isDragged) 1f else 0f)
            .background(MainTheme.colors.surface)
            .padding(start = startPadding, end = MainTheme.spacings.zero)
            .immediateDragGesture(
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
            .semantics {
                this.contentDescription = contentDescription
                customActions = listOf(
                    CustomAccessibilityAction(moveUpLabel) { onMoveUp() },
                    CustomAccessibilityAction(moveDownLabel) { onMoveDown() },
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
