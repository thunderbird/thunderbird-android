package net.thunderbird.feature.mail.message.list.ui.state.sideeffect

import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

abstract class MessageListStateSideEffectHandler(
    logger: Logger,
    dispatch: suspend (MessageListEvent) -> Unit,
    dispatchUiEffect: suspend (MessageListEffect) -> Unit = {},
) : StateSideEffectHandler<MessageListState, MessageListEvent, MessageListEffect>(logger, dispatch, dispatchUiEffect)

interface MessageListStateSideEffectHandlerFactory :
    StateSideEffectHandler.Factory<MessageListState, MessageListEvent, MessageListEffect>
