package net.thunderbird.feature.funding.api

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.configstore.ConfigStore

interface FundingConfigStore : ConfigStore<FundingConfig> {
    fun configAsStateFlow(): StateFlow<FundingConfig>
    suspend fun ConfigStore<FundingConfig>.update(transform: (FundingConfig) -> FundingConfig)
}
