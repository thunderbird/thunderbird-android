package com.fsck.k9.activity

import com.fsck.k9.activity.setup.AuthViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val activityModule = module {
    single { MessageLoaderHelperFactory(messageViewInfoExtractorFactory = get(), htmlSettingsProvider = get()) }
    viewModel { AuthViewModel(application = get(), accountManager = get(), oAuthConfigurationProvider = get()) }
}
