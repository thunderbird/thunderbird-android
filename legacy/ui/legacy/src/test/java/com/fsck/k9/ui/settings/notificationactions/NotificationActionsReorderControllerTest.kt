package com.fsck.k9.ui.settings.notificationactions

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test

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

        assertThat(moved).isTrue()
        assertThat(controller.cutoffIndex).isEqualTo(3)
        assertThat(controller.actionOrder()).isEqualTo(
            listOf(
                MessageNotificationAction.Reply,
                MessageNotificationAction.Archive,
                MessageNotificationAction.Spam,
                MessageNotificationAction.MarkAsRead,
                MessageNotificationAction.Star,
                MessageNotificationAction.Delete,
            ),
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

        assertThat(controller.actionOrder()).isEqualTo(
            listOf(
                MessageNotificationAction.Reply,
                MessageNotificationAction.Archive,
                MessageNotificationAction.MarkAsRead,
                MessageNotificationAction.Spam,
                MessageNotificationAction.Star,
                MessageNotificationAction.Delete,
            ),
        )
        assertThat(controller.cutoffIndex).isEqualTo(3)

        controller.dragBy(
            deltaY = -50f,
            visibleItems = controller.visibleItemsForTest(),
        )

        assertThat(controller.actionOrder()).isEqualTo(
            listOf(
                MessageNotificationAction.Reply,
                MessageNotificationAction.Archive,
                MessageNotificationAction.Spam,
                MessageNotificationAction.MarkAsRead,
                MessageNotificationAction.Star,
                MessageNotificationAction.Delete,
            ),
        )
        assertThat(controller.cutoffIndex).isEqualTo(3)
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
