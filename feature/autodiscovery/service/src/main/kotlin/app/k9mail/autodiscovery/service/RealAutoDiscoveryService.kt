package app.k9mail.autodiscovery.service

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.autoconfig.AutoconfigUrlConfig
import app.k9mail.autodiscovery.autoconfig.createIspDbAutoconfigDiscovery
import app.k9mail.autodiscovery.autoconfig.createMxLookupAutoconfigDiscovery
import app.k9mail.autodiscovery.autoconfig.createProviderAutoconfigDiscovery
import app.k9mail.core.common.mail.EmailAddress
import okhttp3.OkHttpClient

/**
 * Uses Thunderbird's Autoconfig mechanism to find mail server settings for a given email address.
 */
class RealAutoDiscoveryService(
    private val okHttpClient: OkHttpClient,
) : AutoDiscoveryService {

    override suspend fun discover(email: EmailAddress): AutoDiscoveryResult {
        val config = AutoconfigUrlConfig(
            httpsOnly = false,
            includeEmailAddress = false,
        )

        val providerDiscovery = createProviderAutoconfigDiscovery(okHttpClient, config)
        val ispDbDiscovery = createIspDbAutoconfigDiscovery(okHttpClient)
        val mxDiscovery = createMxLookupAutoconfigDiscovery(okHttpClient)

        val runnables = listOf(
            providerDiscovery,
            ispDbDiscovery,
            mxDiscovery,
        ).flatMap { discovery ->
            discovery.initDiscovery(email)
        }

        val runner = PriorityParallelRunner(runnables)
        return runner.run()
    }
}
