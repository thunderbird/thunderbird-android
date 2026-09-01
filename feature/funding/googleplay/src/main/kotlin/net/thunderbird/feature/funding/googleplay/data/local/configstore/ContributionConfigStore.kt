package net.thunderbird.feature.funding.googleplay.data.local.configstore

import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local

internal class ContributionConfigStore(
    provider: ConfigBackendProvider,
    definition: Local.ContributionConfigDefinition,
) : BaseConfigStore<ContributionConfig>(
    provider = provider,
    definition = definition,
),
    Local.ContributionConfigStore
