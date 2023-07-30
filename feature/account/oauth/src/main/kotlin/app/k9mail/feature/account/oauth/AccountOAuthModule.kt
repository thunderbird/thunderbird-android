package app.k9mail.feature.account.oauth

import app.k9mail.core.common.coreCommonModule
import app.k9mail.feature.account.oauth.data.AuthorizationRepository
import app.k9mail.feature.account.oauth.data.AuthorizationStateRepository
import app.k9mail.feature.account.oauth.domain.DomainContract
import app.k9mail.feature.account.oauth.domain.DomainContract.UseCase
import app.k9mail.feature.account.oauth.domain.usecase.CheckIsGoogleSignIn
import app.k9mail.feature.account.oauth.domain.usecase.FinishOAuthSignIn
import app.k9mail.feature.account.oauth.domain.usecase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.domain.usecase.SuggestServerName
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthViewModel
import net.openid.appauth.AuthorizationService
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

    factory<DomainContract.AuthorizationRepository> {
        AuthorizationRepository(
            service = get(),
        )
    }

    factory<DomainContract.AuthorizationStateRepository> {
        AuthorizationStateRepository()
    }

    factory<UseCase.SuggestServerName> { SuggestServerName() }

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
