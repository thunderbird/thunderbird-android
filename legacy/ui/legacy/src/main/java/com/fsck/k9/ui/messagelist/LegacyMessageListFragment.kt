package com.fsck.k9.ui.messagelist

import androidx.core.os.bundleOf
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer

private const val TAG = "LegacyMessageListFragment"

@Deprecated(
    message = "DO NOT introduce any new features in this class. " +
        "This will be replaced by the new MessageListFragment and deleted in the future.",
)
class LegacyMessageListFragment : BaseMessageListFragment() {
    override val logTag: String = TAG

    companion object Factory : BaseMessageListFragment.Factory {
        override fun newInstance(
            search: LocalMessageSearch,
            isThreadDisplay: Boolean,
            threadedList: Boolean,
        ): LegacyMessageListFragment {
            val searchBytes = LocalMessageSearchSerializer.serialize(search)

            return LegacyMessageListFragment().apply {
                arguments = bundleOf(
                    ARG_SEARCH to searchBytes,
                    ARG_IS_THREAD_DISPLAY to isThreadDisplay,
                    ARG_THREADED_LIST to threadedList,
                )
            }
        }
    }
}
