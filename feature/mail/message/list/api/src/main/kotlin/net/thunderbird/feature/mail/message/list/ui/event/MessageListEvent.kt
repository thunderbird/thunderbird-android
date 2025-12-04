package net.thunderbird.feature.mail.message.list.ui.event

import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.SortType

/**
 * Represents the events that can be triggered from the message list screen.
 * These events are handled by the [MessageListViewModel] to update the UI state.
 */
sealed interface MessageListEvent {
    sealed interface SystemEvent : MessageListEvent
    sealed interface UserEvent : MessageListEvent

    /**
     * A system event to trigger the loading of initial configurations for the message list.
     * This includes loading swipe actions, user preferences, and available sort types.
     */
    data object LoadConfigurations : SystemEvent

    /**
     * A system event indicating that the swipe actions for one or more accounts have been loaded.
     *
     * @param swipeActions A map where the key is the [AccountId] and the value is the corresponding [SwipeActions]
     *                     configuration for that account.
     */
    data class SwipeActionsLoaded(val swipeActions: Map<AccountId, SwipeActions>) : SystemEvent

    /**
     * A system event to update the message list preferences.
     * This is typically triggered when preferences change from an external source, like the settings screen.
     *
     * @param preferences The new [MessageListPreferences] to apply.
     */
    data class UpdatePreferences(val preferences: MessageListPreferences) : SystemEvent

    /**
     * A system event indicating that the sort types for various accounts have been loaded.
     *
     * @param sortTypes A map where the key is the [AccountId] and the value is the corresponding [SortType].
     *                  A `null` key represents the global or default sort type.
     */
    data class SortTypesLoaded(val sortTypes: Map<AccountId?, SortType>) : SystemEvent

    /**
     * Signals that all initial configurations, such as preferences, swipe actions, and sort types,
     * have been successfully loaded and applied. This event indicates that the system is ready
     * to proceed with loading the actual message list content.
     */
    data object AllConfigsReady : SystemEvent

    /**
     * A system event to update the progress of the loading indicator.
     *
     * @param progress A float value between 0.0 and 1.0 representing the loading completion percentage.
     */
    data class UpdateLoadingProgress(val progress: Float) : SystemEvent

    /**
     * A system event indicating that a list of messages has been successfully loaded.
     *
     * @param messages The list of [MessageItemUi] objects to be displayed.
     */
    data class MessagesLoaded(val messages: List<MessageItemUi>) : SystemEvent

    /**
     * Event triggered when the user initiates selection mode, usually through a long press on a message item.
     */
    data object EnterSelectionMode : UserEvent

    /**
     * Event triggered when the user exits the message selection mode.
     * This typically happens when the user deselects all messages or cancels the selection action.
     */
    data object ExitSelectionMode : UserEvent

    /**
     * Represents the event of a user requesting to load more messages in the list.
     */
    data object LoadMore : UserEvent

    /**
     * Triggers a refresh of the message list, fetching the latest messages from the server.
     */
    data object Refresh : UserEvent

    /**
     * An event that is triggered when the user changes the sort order of the message list.
     *
     * @param sortType The new [SortType] to apply to the message list.
     */
    data class ChangeSortType(val sortType: SortType) : UserEvent
}
