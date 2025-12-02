package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * A base class for handling side effects that arise from state transitions in the message list UI.
 *
 * Each implementation of this class is responsible for a specific side effect, such as loading more messages,
 * marking messages as read, or showing notifications. The handler decides whether to act based on the
 * incoming event and the resulting state change.
 *
 * @property logger A [Logger] for logging the handler's operations.
 * @property dispatch A function to send new [MessageListEvent]s back to the UI's event loop.
 */
abstract class StateSideEffectHandler(
    private val logger: Logger,
    protected val dispatch: suspend (MessageListEvent) -> Unit,
) {
    /**
     * Determines whether this side effect handler should be triggered.
     *
     * This function is called for every state change to check if the conditions for executing
     * this specific side effect are met.
     *
     * @param event The [MessageListEvent] that triggered the state change.
     * @param newState The new [MessageListState] after the event was processed.
     * @return `true` if this handler should execute its `handle` method, `false` otherwise.
     */
    abstract fun accept(event: MessageListEvent, newState: MessageListState): Boolean

    /**
     * Handles the side effect based on the state transition.
     *
     * This function is invoked when the `accept` method returns `true`, indicating that this handler
     * should process the state change. It's responsible for executing the actual side effect,
     * such as dispatching a new event, logging, or interacting with other system components.
     *
     * @param oldState The state before the event was processed.
     * @param newState The new state after the event has been processed.
     */
    abstract suspend fun handle(oldState: MessageListState, newState: MessageListState)

    /**
     * Handles a state change by checking if this handler should react to the [event] and the [newState].
     * If it should, it calls the [handle] method to perform the side effect.
     *
     * @param event The event that triggered the state change.
     * @param oldState The state before the event was processed.
     * @param newState The state after the event was processed.
     */
    suspend fun handle(event: MessageListEvent, oldState: MessageListState, newState: MessageListState) {
        logger.verbose { "handle() called with: event = $event, oldState = $oldState, newState = $newState" }
        if (accept(event, newState)) {
            handle(oldState, newState)
        }
    }

    interface Factory {
        fun create(scope: CoroutineScope, dispatch: suspend (MessageListEvent) -> Unit): StateSideEffectHandler
    }
}
