package com.fsck.k9.ui.endtoend

import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val endToEndUiModule = applicationContext {
    factory { AutocryptSetupMessageLiveEvent(get()) }
    factory { AutocryptSetupTransferLiveEvent(get()) }
    factory { params ->
        AutocryptKeyTransferPresenter(
                params["lifecycleOwner"],
                get(parameters = { params.values }),
                get(),
                get(),
                params["autocryptTransferView"])
    }
    viewModel { AutocryptKeyTransferViewModel(get(), get()) }
}
