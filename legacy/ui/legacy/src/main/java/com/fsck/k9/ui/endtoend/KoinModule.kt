package com.fsck.k9.ui.endtoend

import androidx.lifecycle.LifecycleOwner
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val endToEndUiModule = module {
    factory { AutocryptSetupMessageLiveEvent(get()) }
    factory { AutocryptSetupTransferLiveEvent(get()) }
    factory { (lifecycleOwner: LifecycleOwner, autocryptTransferView: AutocryptKeyTransferActivity) ->
        AutocryptKeyTransferPresenter(
            lifecycleOwner,
            get { parametersOf(lifecycleOwner) },
            get(),
            get(),
            autocryptTransferView,
        )
    }
    viewModel { AutocryptKeyTransferViewModel(get(), get()) }
}
