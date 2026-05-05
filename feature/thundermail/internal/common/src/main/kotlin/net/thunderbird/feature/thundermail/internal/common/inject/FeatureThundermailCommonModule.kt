package net.thunderbird.feature.thundermail.internal.common.inject

import app.k9mail.feature.account.oauth.ui.AccountOAuthViewModel
import net.thunderbird.feature.thundermail.internal.common.domain.CreateAccountStateUseCase
import net.thunderbird.feature.thundermail.internal.common.navigation.DefaultThundermailNavigation
import net.thunderbird.feature.thundermail.internal.common.ui.ThundermailContract
import net.thunderbird.feature.thundermail.internal.common.ui.ThundermailOAuthViewModel
import net.thunderbird.feature.thundermail.navigation.ThundermailNavigation
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureThundermailCommonModule = module {
    viewModel<AccountOAuthViewModel> {
        AccountOAuthViewModel(
            getOAuthRequestIntent = get(),
            finishOAuthSignIn = get(),
            checkIsGoogleSignIn = get(),
        )
    }
    viewModel<ThundermailContract.ViewModel> {
        ThundermailOAuthViewModel(
            logger = get(),
            accountOAuthViewModel = get(),
            getAutoDiscovery = get(),
            createAccountStateUseCase = get(),
        )
    }
    single<CreateAccountStateUseCase> { CreateAccountStateUseCase(get()) }
    single<ThundermailNavigation> { DefaultThundermailNavigation() }
}
