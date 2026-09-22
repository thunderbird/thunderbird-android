package net.thunderbird.app.common.feature.funding.configstore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.ConfigStore
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigStore

class DefaultFundingConfigStore(
    id: ConfigId,
    provider: ConfigBackendProvider,
    private val scope: CoroutineScope,
) : BaseConfigStore<FundingConfig>(
    provider = provider,
    definition = FundingConfigDefinition(id = id),
),
    FundingConfigStore {

    override fun configAsStateFlow(): StateFlow<FundingConfig> {
        return config.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = FundingConfig.DEFAULT,
        )
    }

    override suspend fun ConfigStore<FundingConfig>.update(
        transform: (FundingConfig) -> FundingConfig,
    ) {
        scope.launch {
            super.update {
                val config = it ?: FundingConfig.DEFAULT
                transform(config)
            }
        }
    }
}
