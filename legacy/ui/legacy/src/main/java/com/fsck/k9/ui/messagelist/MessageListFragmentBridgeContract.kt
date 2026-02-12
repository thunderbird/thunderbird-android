package com.fsck.k9.ui.messagelist

import androidx.appcompat.view.ActionMode
import androidx.fragment.app.FragmentActivity
import app.k9mail.legacy.message.controller.MessageReference
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.message.list.MessageListFeatureFlags
import net.thunderbird.feature.search.legacy.LocalMessageSearch

/**
 * Interface defining a contract for bridging functionalities between `MessageListFragment` and
 * other components like `MessageListHandler` and `MessageHomeActivity`.
 *
 * This interface will help us to bridge functionalities between the legacy and modern implementations
 * of the message list fragment.
 */
@Suppress("TooManyFunctions")
interface MessageListFragmentBridgeContract {
    val legacyViewModel: MessageListViewModel
    var isActive: Boolean
    val fragmentActivity: FragmentActivity?

    // used on both MessageListFragment and MessageListHandler
    fun updateFooterText(text: String?)

    // used on both MessageListFragment and MessageHomeActivity
    fun updateTitle()

    // region [used on MessageListHandler]
    fun progress(progress: Boolean)
    fun folderLoading(folderId: Long, loading: Boolean)
    fun remoteSearchFinished()
    // endregion [used on MessageListHandler]

    // region [ used on MessageHomeActivity ]
    val localSearch: LocalMessageSearch
    val isShowAccountIndicator: Boolean
    fun isSearchViewCollapsed(): Boolean
    fun expandSearchView()
    fun onCompose()
    fun onCycleSort()
    fun collapseSearchView()
    fun onReverseSort()
    fun onDelete()
    fun toggleMessageSelect()
    fun onToggleFlagged()
    fun onToggleRead()
    fun onMove()
    fun onArchive()
    fun onCopy()
    fun finishActionMode()
    fun setActiveMessage(messageReference: MessageReference?)
    fun onFullyActive()
    // endregion [ methods used on MessageHomeActivity ]

    interface MessageListFragmentListener {
        fun setMessageListProgressEnabled(enable: Boolean)
        fun setMessageListProgress(level: Int)
        fun showThread(account: LegacyAccount, threadRootId: Long)
        fun openMessage(messageReference: MessageReference)
        fun setMessageListTitle(title: String, subtitle: String? = null)
        fun onCompose(account: LegacyAccount?)
        fun startSearch(query: String, account: LegacyAccount?, folderId: Long?): Boolean
        fun startSupportActionMode(callback: ActionMode.Callback): ActionMode?
        fun goBack()

        companion object Companion {
            const val MAX_PROGRESS = 10000
        }
    }

    /**
     * A factory for creating instances of [BaseMessageListFragment].
     *
     * This interface is a temporary solution to toggle between different fragment implementations
     * based on a feature flag. It allows for the creation of either a modern [MessageListFragment] or a
     * [LegacyMessageListFragment] depending on the state of [MessageListFeatureFlags.EnableMessageListNewState].
     */
    interface Factory {
        /**
         * Creates a new instance of a class that inherits from [BaseMessageListFragment].
         *
         * The specific implementation returned ([MessageListFragment] or [LegacyMessageListFragment]) is determined
         * by the [MessageListFeatureFlags.EnableMessageListNewState] feature flag.
         *
         * @param search The search query that defines which messages to display.
         * @param isThreadDisplay `true` if the fragment is used to display a single thread, `false` otherwise.
         * @param threadedList `true` to display the message list in a threaded conversation view, `false` otherwise.
         *
         * @return An instance of [MessageListFragment] if the new state feature flag is enabled;
         *  otherwise, an instance of [LegacyMessageListFragment].
         */
        fun newInstance(
            search: LocalMessageSearch,
            isThreadDisplay: Boolean,
            threadedList: Boolean,
        ): MessageListFragmentBridgeContract
    }

    companion object {
        const val ARG_SEARCH = "searchObject"
        const val ARG_THREADED_LIST = "showingThreadedList"
        const val ARG_IS_THREAD_DISPLAY = "isThreadedDisplay"

        const val STATE_SELECTED_MESSAGES = "selectedMessages"
        const val STATE_ACTIVE_MESSAGES = "activeMessages"
        const val STATE_ACTIVE_MESSAGE = "activeMessage"
        const val STATE_REMOTE_SEARCH_PERFORMED = "remoteSearchPerformed"
        const val STATE_SEARCH_VIEW_QUERY = "searchViewQuery"
        const val STATE_SEARCH_VIEW_ICONIFIED = "searchViewIconified"
        const val STATE_MESSAGE_LIST_APPEARANCE = "messageListAppearance"
    }
}
