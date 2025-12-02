package com.fsck.k9.ui.messagelist

import androidx.core.os.bundleOf
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer

private const val TAG = "MessageListFragment"

// TODO: Move this fragment to :feature:mail:message:list once all migration to the new
//       MessageListFragment to MVI is done.
class MessageListFragment : AbstractMessageListFragment() {
    override val logTag: String = TAG

    companion object Factory : AbstractMessageListFragment.Factory {
        override fun newInstance(
            search: LocalMessageSearch,
            isThreadDisplay: Boolean,
            threadedList: Boolean,
        ): MessageListFragment {
            val searchBytes = LocalMessageSearchSerializer.serialize(search)

            return MessageListFragment().apply {
                arguments = bundleOf(
                    ARG_SEARCH to searchBytes,
                    ARG_IS_THREAD_DISPLAY to isThreadDisplay,
                    ARG_THREADED_LIST to threadedList,
                )
            }
        }
    }
}
