package app.k9mail.autodiscovery.service

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryRegistry
import app.k9mail.autodiscovery.autoconfig.AutoconfigUrlConfig
import app.k9mail.autodiscovery.autoconfig.createIspDbAutoconfigDiscovery
import app.k9mail.autodiscovery.autoconfig.createMxLookupAutoconfigDiscovery
import app.k9mail.autodiscovery.autoconfig.createProviderAutoconfigDiscovery
import okhttp3.OkHttpClient

class RealAutoDiscoveryRegistry(
    private val autoDiscoveries: List<AutoDiscovery> = emptyList(),
) : AutoDiscoveryRegistry {

    override fun getAutoDiscoveries(): List<AutoDiscovery> = autoDiscoveries

    companion object {
        val defaultAutoconfigUrlConfig = AutoconfigUrlConfig(
            httpsOnly = false,
            includeEmailAddress = true,
        )

        fun createDefault(
            okHttpClient: OkHttpClient,
            autoconfigUrlConfig: AutoconfigUrlConfig = defaultAutoconfigUrlConfig,
        ): RealAutoDiscoveryRegistry = RealAutoDiscoveryRegistry(
            autoDiscoveries = listOf(
                createProviderAutoconfigDiscovery(
                    okHttpClient = okHttpClient,
                    config = autoconfigUrlConfig,
                ),
                createIspDbAutoconfigDiscovery(
                    okHttpClient = okHttpClient,
                ),
                createMxLookupAutoconfigDiscovery(
                    okHttpClient = okHttpClient,
                ),
            ),
        )
    }
}
