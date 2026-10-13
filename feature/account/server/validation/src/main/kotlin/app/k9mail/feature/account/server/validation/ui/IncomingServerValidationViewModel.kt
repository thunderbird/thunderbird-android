package app.k9mail.feature.account.server.validation.ui

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.validation.domain.OAuthAccountIdentityResolver
import app.k9mail.feature.account.server.validation.domain.ServerValidationDomainContract.UseCase
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider

class IncomingServerValidationViewModel(
    accountStateRepository: AccountDomainContract.AccountStateRepository,
    validateServerSettings: UseCase.ValidateServerSettings,
    authorizationStateRepository: AccountOAuthDomainContract.AuthorizationStateRepository,
    certificateErrorRepository: ServerCertificateDomainContract.ServerCertificateErrorRepository,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    initialState: ServerValidationContract.State? = null,
    accountIdentityResolver: OAuthAccountIdentityResolver? = null,
    oauthConfigurationProvider: OAuthConfigurationProvider? = null,
    applicationId: String = "",
) : BaseServerValidationViewModel(
    accountStateRepository = accountStateRepository,
    validateServerSettings = validateServerSettings,
    authorizationStateRepository = authorizationStateRepository,
    certificateErrorRepository = certificateErrorRepository,
    oAuthViewModel = oAuthViewModel,
    initialState = initialState,
    isIncomingValidation = true,
    accountIdentityResolver = accountIdentityResolver,
    oauthConfigurationProvider = oauthConfigurationProvider,
    applicationId = applicationId,
),
    ServerValidationContract.IncomingViewModel
