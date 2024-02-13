package app.k9mail.autodiscovery.service

import app.k9mail.autodiscovery.api.AutoDiscoveryRegistry
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.core.common.mail.EmailAddress

/**
 * Uses Thunderbird's Autoconfig mechanism to find mail server settings for a given email address.
 */
class RealAutoDiscoveryService(
    private val autoDiscoveryRegistry: AutoDiscoveryRegistry,
) : AutoDiscoveryService {

    override suspend fun discover(email: EmailAddress): AutoDiscoveryResult {
        val runner = PriorityParallelRunner(
            runnables = autoDiscoveryRegistry.getAutoDiscoveries().flatMap { it.initDiscovery(email) },
        )
        return runner.run()
    }
}
