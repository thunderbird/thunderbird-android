package net.thunderbird.feature.mail.message.list.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.ui.compose.common.mvi.BaseStateMachineViewModel
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Defines the contract between the View and the ViewModel for the message list screen.
 *
 * This contract follows the MVI (Model-View-Intent) pattern, specifying the structure for:
 * - **State (`MessageListState`)**: Represents the UI state.
 * - **Events (`MessageListEvent`)**: User actions or other events from the UI.
 * - **Effects (`MessageListEffect`)**: One-time actions for the UI to perform (e.g., navigation, showing a toast).
 */
interface MessageListContract {
    /**
     * The view model for the message list screen.
     *
     * It is responsible for handling the business logic of the message list screen and for providing the
     * [MessageListState] to the UI. It consumes [MessageListEvent]s and produces [MessageListEffect]s.
     *
     * @see BaseViewModel
     * @see MessageListState
     * @see MessageListEvent
     * @see MessageListEffect
     */
    abstract class ViewModel(
        logger: Logger,
        sideEffectHandlersFactories: List<MessageListStateSideEffectHandlerFactory>,
    ) : BaseStateMachineViewModel<MessageListState, MessageListEvent, MessageListEffect>(
        logger,
        sideEffectHandlersFactories,
    )
}

interface MessageListStateSideEffectHandlerFactory : StateSideEffectHandler.Factory<MessageListState, MessageListEvent>
