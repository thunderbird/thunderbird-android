package com.fsck.k9.ui.messagelist

import androidx.core.os.bundleOf
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parameterSetOf

private const val TAG = "MessageListFragment"

// TODO(10322): Move this fragment to :feature:mail:message:list once all migration to the new
//              MessageListFragment to MVI is done.
class MessageListFragment : BaseMessageListFragment() {
    override val logTag: String = TAG

    // TODO(9497): Remove suppression once we start use the new view model.
    @Suppress("UnusedPrivateProperty")
    private val viewModel: MessageListContract.ViewModel by inject {
        decodeArguments()
        parameterSetOf(accountUuids.map { AccountIdFactory.of(it) }.toSet())
    }

    companion object Factory : BaseMessageListFragment.Factory {
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
