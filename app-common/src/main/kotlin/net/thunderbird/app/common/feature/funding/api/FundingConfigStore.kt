package net.thunderbird.app.common.feature.funding.api

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.configstore.ConfigStore

interface FundingConfigStore : ConfigStore<FundingConfig> {
    fun configAsStateFlow(): StateFlow<FundingConfig>
}
