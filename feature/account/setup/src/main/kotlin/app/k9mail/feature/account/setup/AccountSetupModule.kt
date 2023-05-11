package app.k9mail.feature.account.setup

import app.k9mail.feature.account.setup.ui.AccountSetupViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountSetupModule: Module = module {
    viewModel { AccountSetupViewModel() }
}
