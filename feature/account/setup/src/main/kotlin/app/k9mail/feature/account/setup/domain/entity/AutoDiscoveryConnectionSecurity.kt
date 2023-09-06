package app.k9mail.feature.account.setup.domain.entity

import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity

internal typealias AutoDiscoveryConnectionSecurity = app.k9mail.autodiscovery.api.ConnectionSecurity

internal fun AutoDiscoveryConnectionSecurity.toConnectionSecurity(): ConnectionSecurity {
    return when (this) {
        AutoDiscoveryConnectionSecurity.StartTLS -> ConnectionSecurity.StartTLS
        AutoDiscoveryConnectionSecurity.TLS -> ConnectionSecurity.TLS
    }
}
