package com.fsck.k9.ui.messagelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fsck.k9.ui.folders.FoldersLiveDataFactory
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class MessageListViewModelFactory : ViewModelProvider.Factory, KoinComponent {
    private val foldersLiveDataFactory: FoldersLiveDataFactory by inject()


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MessageListViewModel(foldersLiveDataFactory) as T
    }
}
