package com.fsck.k9.ui.messagelist

import androidx.core.os.bundleOf
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer

private const val TAG = "LegacyMessageListFragment"

@Suppress("LargeClass", "TooManyFunctions")
class LegacyMessageListFragment : AbstractMessageListFragment() {
    override val logTag: String = TAG

    companion object {
        fun newInstance(
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
