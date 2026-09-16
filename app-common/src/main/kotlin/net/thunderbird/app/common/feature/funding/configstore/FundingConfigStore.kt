package net.thunderbird.app.common.feature.funding.configstore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.ConfigStore
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

interface FundingConfigStore : ConfigStore<FundingConfig> {
    fun dataStateFlow(): StateFlow<FundingConfig>
}

class DefaultFundingConfigStore(
    id: ConfigId,
    provider: ConfigBackendProvider,
    private val scope: CoroutineScope,
) : BaseConfigStore<FundingConfig>(
    provider = provider,
    definition = FundingConfigDefinition(id = id),
), FundingConfigStore {

    override fun dataStateFlow(): StateFlow<FundingConfig> {
        return config.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = FundingConfig.DEFAULT,
        )
    }
}
