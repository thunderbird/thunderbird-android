package net.thunderbird.feature.mail.message.list.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.ui.compose.common.mvi.BaseStateMachineViewModel
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification
import org.koin.androidx.compose.koinViewModel

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
    @Stable
    abstract class ViewModel(
        logger: Logger,
        sideEffectHandlersFactories: List<MessageListStateSideEffectHandlerFactory>,
    ) : BaseStateMachineViewModel<MessageListState, MessageListEvent, MessageListEffect>(
        logger,
        sideEffectHandlersFactories,
    ) {
        data class Args(
            val accountIds: Set<AccountId>,
            val folderId: Long?,
        )
    }

    /**
     * Defines the contract for rendering the message list screen user interface.
     *
     * This interface provides `Composable` functions to render the UI based on the provided state
     * and to handle user interactions by dispatching events.
     */
    interface MessageListScreenRenderer {
        /**
         * Renders the message list screen.
         *
         * This is the core composable for displaying the message list UI. It is stateless and relies on the
         * provided `state` to render the UI. User interactions are communicated via the `dispatchEvent` function.
         *
         * @param state The current state of the message list to be rendered.
         * @param dispatchEvent A lambda function to be invoked when a user action or other UI event occurs.
         * @param onEffect A lambda function to handle one-time side effects from the ViewModel.
         * @param preferences User preferences for the message list, influencing its appearance and behavior.
         * @param modifier The modifier to be applied to the root container of the message list screen.
         * @param inAppNotificationEventFilter A filter to decide whether an in-app notification should be displayed.
         */
        @Composable
        fun Render(
            state: MessageListState,
            dispatchEvent: (MessageListEvent) -> Unit,
            onEffect: (MessageListEffect) -> Unit,
            preferences: MessageListPreferences,
            modifier: Modifier = Modifier,
            inAppNotificationEventFilter: (InAppNotification) -> Boolean = { true },
        )

        /**
         * Renders the message list screen user interface.
         *
         * This is a convenience overload of [Render] that automatically retrieves the [ViewModel]
         * using Koin and observes its state.
         *
         * @param preferences The user's preferences for the message list.
         * @param onEffect A callback to handle one-time side effects from the [ViewModel], such as navigation.
         * @param modifier The modifier to be applied to the layout.
         * @param viewModel The [ViewModel] instance for this screen. Defaults to the instance provided by Koin.
         * @param inAppNotificationEventFilter A filter to decide whether an in-app notification should be displayed.
         */
        @Composable
        fun Render(
            preferences: MessageListPreferences,
            onEffect: (MessageListEffect) -> Unit,
            modifier: Modifier = Modifier,
            viewModel: ViewModel = koinViewModel(),
            inAppNotificationEventFilter: (InAppNotification) -> Boolean = { true },
        ) {
            val (state, dispatchEvent) = viewModel.observe(onEffect)
            Render(state.value, dispatchEvent, onEffect, preferences, modifier, inAppNotificationEventFilter)
        }
    }
}

interface MessageListStateSideEffectHandlerFactory : StateSideEffectHandler.Factory<MessageListState, MessageListEvent>
