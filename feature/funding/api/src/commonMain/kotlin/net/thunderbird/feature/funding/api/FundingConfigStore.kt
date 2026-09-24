package net.thunderbird.feature.funding.api

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.configstore.ConfigStore

interface FundingConfigStore : ConfigStore<FundingConfig> {
    val configStateFlow: StateFlow<FundingConfig>
    suspend fun ConfigStore<FundingConfig>.safeUpdate(transform: (FundingConfig) -> FundingConfig)
}
