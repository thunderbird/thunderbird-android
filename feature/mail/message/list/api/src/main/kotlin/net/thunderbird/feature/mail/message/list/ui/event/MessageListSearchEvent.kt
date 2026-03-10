package net.thunderbird.feature.mail.message.list.ui.event

/**
 * Defines the events related to searching, extending [MessageListEvent].
 * These events are typically triggered by user interactions or specific conditions.
 *
 * @see MessageListEvent
 */
sealed interface MessageListSearchEvent : MessageListEvent.UserEvent {
    /**
     * Event triggered when a user performs a search with a specific query.
     *
     * @param query The text string to search for within the message list.
     */
    data class UpdateSearchQuery(val query: String) : MessageListSearchEvent

    /**
     * Represents an event to trigger a "search everywhere" action.
     * This typically expands the search scope to include all folders or accounts,
     * not just the currently viewed one.
     */
    data object SearchEverywhere : MessageListSearchEvent

    /**
     * Event to trigger a remote search on the server for the current query.
     */
    data object SearchRemotely : MessageListSearchEvent

    /**
     * Event triggered to enter in the search mode.
     */
    data object EnterSearchMode : MessageListSearchEvent

    /**
     * Event triggered to clear the current search query and exit the search mode.
     */
    data object ExitSearchMode : MessageListSearchEvent
}
