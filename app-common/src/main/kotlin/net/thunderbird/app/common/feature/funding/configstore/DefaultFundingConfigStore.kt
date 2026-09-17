package net.thunderbird.app.common.feature.funding.configstore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.app.common.feature.funding.api.FundingConfig
import net.thunderbird.app.common.feature.funding.api.FundingConfigDefinition
import net.thunderbird.app.common.feature.funding.api.FundingConfigStore
import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

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
}
