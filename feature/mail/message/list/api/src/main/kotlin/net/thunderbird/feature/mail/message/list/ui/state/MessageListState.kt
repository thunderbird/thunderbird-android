package net.thunderbird.feature.mail.message.list.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences

/**
 * Represents the state of the message list screen.
 *
 * This sealed interface defines the different possible states, such as when messages are being loaded,
 * have been loaded, are being searched, or when the user is selecting messages for an action.
 *
 * @property folder The current folder being displayed. `null` if no folder is selected (e.g. Unified Inbox).
 * @property messages An immutable list of [MessageItemUi] objects to be displayed.
 * @property preferences User-configurable preferences for the message list display.
 * @property swipeActions The swipe actions configured for the message list.
 * @property selectedSortTypes The currently selected sorting order for the messages.
 * @property activeMessage The message that is currently being viewed in a split-screen or tablet layout.
 * @property isActive `true` if the message list is the currently active screen; `false` otherwise.
 */
@Immutable
sealed interface MessageListState {
    val folder: Folder?
    val messages: ImmutableList<MessageItemUi>
    val preferences: MessageListPreferences?
    val swipeActions: ImmutableMap<AccountId, SwipeActions>
    val selectedSortTypes: ImmutableMap<AccountId?, SortType>
    val activeMessage: MessageItemUi?
    val isActive: Boolean

    /**
     * Represents the initial state of the message list screen before any messages are loaded.
     *
     * This state is used during the initial setup or "warm-up" phase, where the UI is being
     * prepared but no data fetching has been initiated yet. It provides default values for
     * the UI to display a consistent initial view.
     */
    data class WarmingUp(
        override val folder: Folder? = null,
        override val messages: ImmutableList<MessageItemUi> = persistentListOf(),
        override val swipeActions: ImmutableMap<AccountId, SwipeActions> = persistentMapOf(),
        override val selectedSortTypes: ImmutableMap<AccountId?, SortType> = persistentMapOf(),
        override val activeMessage: MessageItemUi? = null,
        override val preferences: MessageListPreferences? = null,
        override val isActive: Boolean = false,
    ) : MessageListState {
        val isReady: Boolean
            get() = swipeActions.isNotEmpty() && preferences != null && selectedSortTypes.isNotEmpty()
    }

    /**
     * Represents the state where messages for a folder have been successfully loaded and are ready to be displayed.
     *
     * This is the primary "idle" or "ready" state for the message list.
     */
    data class LoadedMessages(
        override val folder: Folder?,
        override val messages: ImmutableList<MessageItemUi>,
        override val preferences: MessageListPreferences,
        override val swipeActions: ImmutableMap<AccountId, SwipeActions>,
        override val selectedSortTypes: ImmutableMap<AccountId?, SortType> = persistentMapOf(),
        override val activeMessage: MessageItemUi? = null,
        override val isActive: Boolean = true,
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
        override val folder: Folder?,
        override val messages: ImmutableList<MessageItemUi> = persistentListOf(),
        override val preferences: MessageListPreferences,
        override val swipeActions: ImmutableMap<AccountId, SwipeActions>,
        override val selectedSortTypes: ImmutableMap<AccountId?, SortType> = persistentMapOf(),
        override val activeMessage: MessageItemUi? = null,
        override val isActive: Boolean = true,
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
        override val folder: Folder?,
        override val messages: ImmutableList<MessageItemUi>,
        override val preferences: MessageListPreferences,
        override val swipeActions: ImmutableMap<AccountId, SwipeActions>,
        override val selectedSortTypes: ImmutableMap<AccountId?, SortType> = persistentMapOf(),
        override val activeMessage: MessageItemUi? = null,
        override val isActive: Boolean = true,
    ) : MessageListState

    /**
     * Represents the state where the user is actively selecting one or more messages to perform a bulk action
     * (e.g., delete, archive, mark as read).
     *
     * This state is typically entered when a user long-presses a message or taps the selection checkbox,
     * enabling a multi-select mode in the UI.
     */
    data class SelectingMessages(
        override val folder: Folder?,
        override val messages: ImmutableList<MessageItemUi>,
        override val preferences: MessageListPreferences,
        override val swipeActions: ImmutableMap<AccountId, SwipeActions>,
        override val selectedSortTypes: ImmutableMap<AccountId?, SortType> = persistentMapOf(),
        override val activeMessage: MessageItemUi? = null,
        override val isActive: Boolean = true,
    ) : MessageListState {
        val selectedCount: Int = messages.count { it.selected }
    }
}
