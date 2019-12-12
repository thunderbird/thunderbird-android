package com.fsck.k9.ui.messagelist

import org.koin.dsl.module

val messageListUiModule = module {
    factory { DefaultFolderProvider() }
}
