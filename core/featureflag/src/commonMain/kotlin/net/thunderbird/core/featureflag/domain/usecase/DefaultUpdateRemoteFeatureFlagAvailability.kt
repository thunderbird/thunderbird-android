package net.thunderbird.core.featureflag.domain.usecase

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.safeUpdate
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.UpdateRemoteFeatureFlagAvailability

class DefaultUpdateRemoteFeatureFlagAvailability(
    private val configStore: FeatureFlagConfigStore,
) : UpdateRemoteFeatureFlagAvailability {
    override suspend fun invoke(enabled: Boolean): Outcome<Unit, UpdateRemoteFeatureFlagAvailability.Failure> {
        configStore.safeUpdate { it.copy(remoteCatalogConfig = it.remoteCatalogConfig.copy(enabled = enabled)) }
        return Outcome.success()
    }
}
