package app.k9mail.feature.account.oauth

import app.k9mail.feature.account.oauth.data.AuthorizationRepository
import app.k9mail.feature.account.oauth.data.AuthorizationStateRepository
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract.UseCase
import app.k9mail.feature.account.oauth.domain.usecase.CheckIsGoogleSignIn
import app.k9mail.feature.account.oauth.domain.usecase.FinishOAuthSignIn
import app.k9mail.feature.account.oauth.domain.usecase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthViewModel
import net.openid.appauth.AuthorizationService
import net.thunderbird.core.common.coreCommonModule
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountOAuthModule: Module = module {
    includes(coreCommonModule)

    factory {
        AuthorizationService(
            androidApplication(),
        )
    }

    factory<AccountOAuthDomainContract.AuthorizationRepository> {
        AuthorizationRepository(
            service = get(),
        )
    }

    factory<AccountOAuthDomainContract.AuthorizationStateRepository> {
        AuthorizationStateRepository()
    }

    factory<UseCase.GetOAuthRequestIntent> {
        GetOAuthRequestIntent(
            repository = get(),
            configurationProvider = get(),
        )
    }

    factory<UseCase.FinishOAuthSignIn> { FinishOAuthSignIn(repository = get()) }

    factory<UseCase.CheckIsGoogleSignIn> { CheckIsGoogleSignIn() }

    factory<AccountOAuthContract.ViewModel> {
        AccountOAuthViewModel(
            getOAuthRequestIntent = get(),
            finishOAuthSignIn = get(),
            checkIsGoogleSignIn = get(),
        )
    }
}
