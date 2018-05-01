package com.fsck.k9.ui.endtoend

import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val endToEndUiModule = applicationContext {
    factory { AutocryptSetupMessageLiveEvent() }
    factory { AutocryptSetupTransferLiveEvent() }
    viewModel { AutocryptKeyTransferViewModel(get(), get()) }
}