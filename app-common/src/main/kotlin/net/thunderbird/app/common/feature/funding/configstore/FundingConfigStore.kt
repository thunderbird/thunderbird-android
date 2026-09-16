package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.ConfigStore
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

interface FundingConfigStore : ConfigStore<FundingConfig>

class DefaultFundingConfigStore(
    id: ConfigId,
    provider: ConfigBackendProvider,
) : BaseConfigStore<FundingConfig>(
    provider = provider,
    definition = FundingConfigDefinition(id = id),
), FundingConfigStore
