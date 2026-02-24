package com.fsck.k9.ui.messagelist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parameterSetOf

private const val TAG = "MessageListFragment"

// TODO(#10322): Move this fragment to :feature:mail:message:list once all migration to the new
//              MessageListFragment to MVI is done.
@SuppressLint("DiscouragedApi")
class MessageListFragment : BaseMessageListFragment() {
    override val logTag: String = TAG

    private val viewModel: MessageListContract.ViewModel by inject {
        decodeArguments()
        parameterSetOf(accountUuids.map { AccountIdFactory.of(it) }.toSet())
    }

    override val swipeActions: StateFlow<Map<AccountId, SwipeActions>> by lazy {
        viewModel
            .state
            .map { it.metadata.swipeActions }
            .stateIn(lifecycleScope, SharingStarted.Lazily, emptyMap())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        else -> Unit
                    }
                }
            }
        }
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
