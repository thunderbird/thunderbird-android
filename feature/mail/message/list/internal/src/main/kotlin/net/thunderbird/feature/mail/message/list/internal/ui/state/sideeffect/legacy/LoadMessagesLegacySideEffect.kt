package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.legacy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.legacy.LegacyMessageListBridge
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

private const val TAG = "LoadMessagesLegacySideEffect"

/**
 * Side effect handler responsible for loading messages through the legacy message list bridge.
 *
 * @property scope The coroutine scope in which the message loading operation will be launched
 * @property logger Logger instance for tracking loading operations and debugging
 * @property legacyBridge Bridge to the legacy message list system for loading messages
 * @property dispatch Function to dispatch events back to the state machine
 */
internal class LoadMessagesLegacySideEffect(
    private val scope: CoroutineScope,
    private val logger: Logger,
    private val legacyBridge: LegacyMessageListBridge,
    dispatch: suspend (MessageListEvent) -> Unit,
) : MessageListStateSideEffectHandler(logger, dispatch) {
    private var running: Job? = null
    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        oldState != newState && newState is MessageListState.LoadingMessages

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        if (newState !is MessageListState.LoadingMessages) return ConsumeResult.Ignored
        running?.cancel()
        running = legacyBridge
            .loadMessages(newState.preferences, newState.metadata)
            .onEach { messages ->
                logger.verbose(TAG) { "LoadMessagesLegacySideEffect.handle() messages: $messages" }
                // this is oversimplified as we currently don't track the loading progress
                // in the legacy implementation.
                val progress = if (messages.isEmpty()) 0f else 1f
                dispatch(MessageListEvent.UpdateLoadingProgress(progress = progress, messages = messages))
            }
            .onCompletion { running = null }
            .catch { throwable ->
                logger.error(TAG) { "LoadMessagesLegacySideEffect failed: $throwable" }
            }
            .launchIn(scope)
        return ConsumeResult.Consumed
    }

    class Factory(
        private val logger: Logger,
        private val legacyBridge: LegacyMessageListBridge,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = LoadMessagesLegacySideEffect(
            scope = scope,
            logger = logger,
            legacyBridge = legacyBridge,
            dispatch = dispatch,
        )
    }
}
