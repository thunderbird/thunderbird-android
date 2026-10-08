package net.thunderbird.core.common.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger

/**
 * A class for handling side effects that arise when state transitions to another state.
 *
 * Each implementation of this class is responsible for a specific side effect, such as loading more messages,
 * marking messages as read, or showing notifications. The handler decides whether to act based on the
 * incoming event and the resulting state change.
 *
 * @property logger A [Logger] for logging the handler's operations.
 * @property dispatch A function to send new [TEvent]s back to the UI's event loop.
 */
abstract class StateSideEffectHandler<TState : Any, in TEvent : Any, TEffect : Any>(
    private val logger: Logger,
    protected val dispatch: suspend (TEvent) -> Unit,
    protected val dispatchUiEffect: suspend (TEffect) -> Unit = {},
) {
    /**
     * Determines whether this side effect handler should be triggered.
     *
     * This function is called for every state change to check if the conditions for executing
     * this specific side effect are met.
     *
     * @param event The [TEvent] that triggered the state change.
     * @param newState The new [TState] after the event was processed.
     * @return `true` if this handler should execute its `handle` method, `false` otherwise.
     */
    protected abstract fun accept(event: TEvent, oldState: TState, newState: TState): Boolean

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
    protected abstract suspend fun consume(event: TEvent, oldState: TState, newState: TState): ConsumeResult

    /**
     * Handles a state change by checking if this handler should react to the [event] and the [newState].
     * If it should, it calls the [handle] method to perform the side effect.
     *
     * @param event The event that triggered the state change.
     * @param oldState The state before the event was processed.
     * @param newState The state after the event was processed.
     */
    suspend fun handle(event: TEvent, oldState: TState, newState: TState): ConsumeResult {
        return if (accept(event, oldState, newState)) {
            logger.verbose {
                """${this::class.simpleName}.handle() called with:
                    |   event = $event,
                    |   oldState = $oldState,
                    |   newState = $newState,
                """.trimMargin()
            }
            consume(event, oldState, newState)
        } else {
            ConsumeResult.Ignored
        }
    }

    fun interface Factory<TState : Any, TEvent : Any, TEffect : Any> {
        fun create(
            scope: CoroutineScope,
            dispatch: suspend (TEvent) -> Unit,
            dispatchUiEffect: suspend (TEffect) -> Unit,
        ): StateSideEffectHandler<TState, TEvent, TEffect>
    }

    sealed interface ConsumeResult {
        data object Ignored : ConsumeResult
        data object Consumed : ConsumeResult
        data object Failure : ConsumeResult
    }
}
