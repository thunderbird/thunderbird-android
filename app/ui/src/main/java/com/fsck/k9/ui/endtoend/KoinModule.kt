package com.fsck.k9.ui.endtoend

import androidx.lifecycle.LifecycleOwner
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.core.parameter.ParameterDefinition
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module.module


val endToEndUiModule = module {
    factory { AutocryptSetupMessageLiveEvent(get()) }
    factory { AutocryptSetupTransferLiveEvent(get()) }
    factory { (lifecycleOwner: LifecycleOwner, activity: AutocryptKeyTransferActivity) ->
        AutocryptKeyTransferPresenter(
                lifecycleOwner,
                get(parameters = parametersOf(lifecycleOwner) as ParameterDefinition),
                get(),
                get(),
                activity)
    }
    viewModel { AutocryptKeyTransferViewModel(get(), get()) }
}
