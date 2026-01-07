package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "LoadedConfigSideEffectHandler"

class LoadedConfigStateSideEffectHandler(
    private val logger: Logger,
    dispatch: suspend (MessageListEvent) -> Unit,
) : StateSideEffectHandler(logger, dispatch) {
    override fun accept(event: MessageListEvent, newState: MessageListState): Boolean =
        newState is MessageListState.WarmingUp && newState.isReady

    override suspend fun handle(oldState: MessageListState, newState: MessageListState) {
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        dispatch(MessageListEvent.AllConfigsReady)
    }

    class Factory(private val logger: Logger) : StateSideEffectHandler.Factory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
        ): StateSideEffectHandler = LoadedConfigStateSideEffectHandler(logger, dispatch)
    }
}
