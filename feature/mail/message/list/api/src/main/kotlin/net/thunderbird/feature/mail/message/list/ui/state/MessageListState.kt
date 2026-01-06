package net.thunderbird.feature.mail.message.list.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences

/**
 * Represents the state of the message list screen.
 *
 * This sealed interface defines the different possible states, such as when messages are being loaded,
 * have been loaded, are being searched, or when the user is selecting messages for an action.
 *
 * @property preferences User-configurable preferences for the message list display.
 * @property messages An immutable list of [MessageItemUi] objects to be displayed.
 */
@Immutable
sealed interface MessageListState {
    val metadata: MessageListMetadata
    val preferences: MessageListPreferences?
    val messages: ImmutableList<MessageItemUi>

    /**
     * Creates a copy of the current state with updated [metadata], preserving all other
     * properties.
     *
     * This is a convenience function to immutably update the state with new [metadata]
     * without needing to manually copy all other properties. It ensures that the specific
     * type of the state (e.g., [LoadedMessages], [SearchingMessages]) is preserved.
     *
     * This is useful for modifying shared properties without needing to handle each state
     * subtype individually.
     *
     * @param transform A lambda function that receives the current [MessageListMetadata]
     * and returns a new, transformed instance.
     * @return A new [MessageListState] instance of the same type as the original, but with
     * the updated metadata.
     */
    fun withMetadata(
        transform: MessageListMetadata.() -> MessageListMetadata,
    ): MessageListState = when (this) {
        is LoadedMessages -> copy(metadata = metadata.transform())
        is LoadingMessages -> copy(metadata = metadata.transform())
        is SearchingMessages -> copy(metadata = metadata.transform())
        is SelectingMessages -> copy(metadata = metadata.transform())
        is WarmingUp -> copy(metadata = metadata.transform())
    }

    /**
     * Creates a copy of the current state with updated [preferences], preserving all other
     * properties.
     *
     * This is a convenience function to immutably update the state with new [preferences]
     * without needing to manually copy all other properties. It ensures that the specific
     * type of the state (e.g., [LoadedMessages], [SearchingMessages]) is preserved.
     *
     * This is particularly useful when preferences change (e.g., user toggles conversation mode)
     * and the UI needs to be recomposed with the new settings while maintaining the rest of the
     * current state like the list of messages, selected folder, etc.
     *
     * @param transform A lambda function that receives the current [MessageListPreferences]
     * and returns a new, transformed instance.
     * @return A new [MessageListState] instance of the same type as the original, but with
     * the updated preferences.
     */
    fun withPreferences(
        transform: MessageListPreferences.() -> MessageListPreferences,
    ): MessageListState = when (this) {
        is LoadedMessages -> copy(preferences = preferences.transform())
        is LoadingMessages -> copy(preferences = preferences.transform())
        is SearchingMessages -> copy(preferences = preferences.transform())
        is SelectingMessages -> copy(preferences = preferences.transform())
        is WarmingUp -> copy(preferences = preferences?.transform())
    }

    /**
     * Represents the initial state of the message list screen before any messages are loaded.
     *
     * This state is used during the initial setup or "warm-up" phase, where the UI is being
     * prepared but no data fetching has been initiated yet. It provides default values for
     * the UI to display a consistent initial view.
     */
    data class WarmingUp(
        override val metadata: MessageListMetadata = MessageListMetadata(
            folder = null,
            swipeActions = persistentMapOf(),
            selectedSortTypes = persistentMapOf(),
            activeMessage = null,
            isActive = false,
        ),
        override val preferences: MessageListPreferences? = null,
        override val messages: ImmutableList<MessageItemUi> = persistentListOf(),
    ) : MessageListState {
        val isReady: Boolean
            get() = metadata.swipeActions.isNotEmpty() && preferences != null && metadata.selectedSortTypes.isNotEmpty()
    }

    /**
     * Represents the state where messages for a folder have been successfully loaded and are ready to be displayed.
     *
     * This is the primary "idle" or "ready" state for the message list.
     */
    data class LoadedMessages(
        override val metadata: MessageListMetadata,
        override val preferences: MessageListPreferences,
        override val messages: ImmutableList<MessageItemUi>,
    ) : MessageListState

    /**
     * Represents the state where messages are being loaded.
     *
     * This state is active when the app is fetching new messages from a local or remote source.
     *
     * @param isPullToRefresh `true` if loading was triggered by a pull-to-refresh gesture, `false` otherwise.
     * @param isRemoteLoading `true` if messages are being fetched from the remote server, `false` for local loading.
     * @param progress A value between 0.0 and 1.0 indicating the loading progress.
     */
    data class LoadingMessages(
        val progress: Float,
        val isPullToRefresh: Boolean = false,
        val isRemoteLoading: Boolean = false,
        override val metadata: MessageListMetadata,
        override val preferences: MessageListPreferences,
        override val messages: ImmutableList<MessageItemUi> = persistentListOf(),
    ) : MessageListState

    /**
     * Represents the state when the user is actively searching for messages.
     *
     * This state is triggered when the user enters a query in the search bar. The message list will display
     * results matching the query, either from the local database or by performing a search on the server.
     *
     * @param searchQuery The text query entered by the user.
     * @param isServerSearch `true` if the search is being performed on the mail server; `false` if it's a local search.
     */
    data class SearchingMessages(
        val searchQuery: String,
        val isServerSearch: Boolean,
        override val metadata: MessageListMetadata,
        override val preferences: MessageListPreferences,
        override val messages: ImmutableList<MessageItemUi>,
    ) : MessageListState

    /**
     * Represents the state where the user is actively selecting one or more messages to perform a bulk action
     * (e.g., delete, archive, mark as read).
     *
     * This state is typically entered when a user long-presses a message or taps the selection checkbox,
     * enabling a multi-select mode in the UI.
     */
    data class SelectingMessages(
        override val metadata: MessageListMetadata,
        override val preferences: MessageListPreferences,
        override val messages: ImmutableList<MessageItemUi>,
    ) : MessageListState {
        val selectedCount: Int = messages.count { it.selected }
    }
}
