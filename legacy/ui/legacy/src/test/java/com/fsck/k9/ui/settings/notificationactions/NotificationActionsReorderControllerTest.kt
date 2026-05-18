package com.fsck.k9.ui.settings.notificationactions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationActionsReorderControllerTest {
    private val initialActions = listOf(
        MessageNotificationAction.Reply,
        MessageNotificationAction.Archive,
        MessageNotificationAction.MarkAsRead,
        MessageNotificationAction.Spam,
        MessageNotificationAction.Star,
        MessageNotificationAction.Delete,
    )

    @Test
    fun `moveByStep crossing into full visible section kicks previous last visible below cutoff`() {
        val controller = createController()

        val moved = controller.moveByStep(
            itemKey = controller.keyForAction(MessageNotificationAction.Spam),
            delta = -1,
        )

        assertTrue(moved)
        assertEquals(3, controller.cutoffIndex)
        assertEquals(
            listOf(
                MessageNotificationAction.Reply,
                MessageNotificationAction.Archive,
                MessageNotificationAction.Spam,
                MessageNotificationAction.MarkAsRead,
                MessageNotificationAction.Star,
                MessageNotificationAction.Delete,
            ),
            controller.actionOrder(),
        )
    }

    @Test
    fun `dragBy crossing from below when full requires last-above threshold before swap`() {
        val controller = createController()
        val draggedAction = MessageNotificationAction.Spam
        controller.startDrag(controller.keyForAction(draggedAction))

        controller.dragBy(
            deltaY = -120f,
            visibleItems = controller.visibleItemsForTest(),
        )

        assertEquals(
            listOf(
                MessageNotificationAction.Reply,
                MessageNotificationAction.Archive,
                MessageNotificationAction.MarkAsRead,
                MessageNotificationAction.Spam,
                MessageNotificationAction.Star,
                MessageNotificationAction.Delete,
            ),
            controller.actionOrder(),
        )
        assertEquals(3, controller.cutoffIndex)

        controller.dragBy(
            deltaY = -50f,
            visibleItems = controller.visibleItemsForTest(),
        )

        assertEquals(
            listOf(
                MessageNotificationAction.Reply,
                MessageNotificationAction.Archive,
                MessageNotificationAction.Spam,
                MessageNotificationAction.MarkAsRead,
                MessageNotificationAction.Star,
                MessageNotificationAction.Delete,
            ),
            controller.actionOrder(),
        )
        assertEquals(3, controller.cutoffIndex)
    }

    private fun createController(): NotificationActionsReorderController {
        return NotificationActionsReorderController(
            initialActions = initialActions,
            initialCutoff = 3,
            onStateChanged = { _, _ -> },
        )
    }

    private fun NotificationActionsReorderController.actionOrder(): List<MessageNotificationAction> {
        return items
            .filterIsInstance<NotificationListItem.Action>()
            .map { it.action }
    }

    private fun NotificationActionsReorderController.keyForAction(action: MessageNotificationAction): String {
        return items
            .filterIsInstance<NotificationListItem.Action>()
            .first { it.action == action }
            .key
    }

    private fun NotificationActionsReorderController.visibleItemsForTest(): List<ReorderVisibleItem> {
        return items.mapIndexed { index, item ->
            ReorderVisibleItem(
                key = item.key,
                offset = index * 100,
                size = 100,
            )
        }
    }
}
