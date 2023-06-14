package app.k9mail.feature.account.setup.domain

import app.k9mail.autodiscovery.api.AutoDiscoveryResult

interface DomainContract {

    fun interface GetAutoDiscoveryUseCase {
        suspend fun execute(emailAddress: String): AutoDiscoveryResult
    }
}
