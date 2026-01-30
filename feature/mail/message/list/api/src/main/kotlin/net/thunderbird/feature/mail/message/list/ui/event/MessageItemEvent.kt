package net.thunderbird.feature.mail.message.list.ui.event

import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

/**
 * Represents events that can be triggered on a single message item in the message list, extending [MessageListEvent].
 *
 * @see MessageListEvent
 */
sealed interface MessageItemEvent : MessageListEvent.UserEvent {
    /**
     * Represents an event triggered when a user clicks on a single message item in the list.
     *
     * @param message The [MessageItemUi] that was clicked.
     */
    data class OnMessageClick(val message: MessageItemUi) : MessageItemEvent

    /**
     * Event to toggle the selection state of one or more messages.
     *
     * @param messages The list of messages whose selection state should be toggled.
     */
    data class ToggleSelectMessages(val messages: List<MessageItemUi>) : MessageItemEvent {
        constructor(message: MessageItemUi) : this(messages = listOf(message))
    }

    /**
     * Event to toggle the 'favourite' (starred) state of one or more messages.
     *
     * @param messages The list of [MessageItemUi]s to be toggled.
     */
    data class ToggleFavourite(val messages: List<MessageItemUi>) : MessageItemEvent {
        constructor(message: MessageItemUi) : this(messages = listOf(message))
    }

    /**
     * An event to toggle the read/unread status of one or more messages.
     *
     * @param messages The list of messages whose read/unread status should be toggled.
     */
    data class ToggleReadUnread(val messages: List<MessageItemUi>) : MessageItemEvent {
        constructor(message: MessageItemUi) : this(messages = listOf(message))
    }

    /**
     * Flags a list of messages with the given [Flag].
     *
     * @param messages The list of messages to flag.
     * @param flag The flag to apply to the messages.
     */
    data class FlagMessages(val messages: List<MessageItemUi>, val flag: Flag) : MessageItemEvent {
        constructor(message: MessageItemUi, flag: Flag) : this(messages = listOf(message), flag = flag)
    }

    /**
     * Represents a swipe action on a message item.
     *
     * @property message The message item that was swiped.
     */
    data class OnSwipeMessage(val message: MessageItemUi, val swipeAction: SwipeAction) : MessageItemEvent
}
