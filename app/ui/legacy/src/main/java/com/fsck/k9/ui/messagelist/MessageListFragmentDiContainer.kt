package com.fsck.k9.ui.messagelist

import com.fsck.k9.fragment.MessageListFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class MessageListFragmentDiContainer(fragment: MessageListFragment) {
    val viewModel: MessageListViewModel by fragment.viewModel()
}
