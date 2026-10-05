package net.thunderbird.core.featureflag.domain.usecase

import kotlinx.io.IOException
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.safeUpdate
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.UpdateRemoteFeatureFlagAvailability

class DefaultUpdateRemoteFeatureFlagAvailability(
    private val configStore: FeatureFlagConfigStore,
) : UpdateRemoteFeatureFlagAvailability {
    override suspend fun invoke(enabled: Boolean): Outcome<Unit, UpdateRemoteFeatureFlagAvailability.Failure> {
        return try {
            configStore.safeUpdate { it.copy(remoteCatalogConfig = it.remoteCatalogConfig.copy(enabled = enabled)) }
            Outcome.success()
        } catch (e: IOException) {
            Outcome.failure(UpdateRemoteFeatureFlagAvailability.Failure.ConfigUpdateFailed(cause = e))
        }
    }
}
