package net.thunderbird.core.featureflag.domain

import kotlinx.coroutines.flow.Flow
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.FeatureFlagKey

interface RemoteFeatureFlagDomainContract {
    fun interface FetchEnabledFeatureFlags {
        operator fun invoke(): Flow<Outcome<Success, Failure>>

        data class Success(
            val enabled: Boolean,
            val isRuntimeOverride: Boolean,
            val flags: List<FeatureFlagKey>,
        )

        sealed interface Failure
    }

    fun interface UpdateRemoteFeatureFlagAvailability {
        suspend operator fun invoke(enabled: Boolean): Outcome<Unit, Failure>
        sealed interface Failure
    }
}
