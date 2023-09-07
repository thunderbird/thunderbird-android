package app.k9mail.feature.account.setup.domain.entity

import app.k9mail.feature.account.common.domain.entity.AuthenticationType

typealias AutoDiscoveryAuthenticationType = app.k9mail.autodiscovery.api.AuthenticationType

internal fun AutoDiscoveryAuthenticationType.toAuthenticationType(): AuthenticationType {
    return when (this) {
        AutoDiscoveryAuthenticationType.PasswordCleartext -> AuthenticationType.PasswordCleartext
        AutoDiscoveryAuthenticationType.PasswordEncrypted -> AuthenticationType.PasswordEncrypted
        AutoDiscoveryAuthenticationType.OAuth2 -> AuthenticationType.OAuth2
    }
}
