package net.thunderbird.core.featureflag.domain

import kotlinx.coroutines.flow.Flow
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.FeatureFlagKey

interface RemoteFeatureFlagDomainContract {
    fun interface FetchEnabledFeatureFlags {
        operator fun invoke(): Flow<Outcome<Success, Failure>>

        data class Success(
            val enabled: Boolean,
            val available: Boolean,
            val isRuntimeOverride: Boolean,
            val flags: List<FeatureFlagKey>,
        )

        sealed interface Failure {
            /**
             * The feature flag configuration could not be read from storage.
             *
             * The flow completes after emitting this failure.
             */
            data class ConfigReadFailed(val cause: Throwable) : Failure
        }
    }

    fun interface UpdateRemoteFeatureFlagAvailability {
        suspend operator fun invoke(enabled: Boolean): Outcome<Unit, Failure>
        sealed interface Failure {
            /** The remote catalog availability could not be persisted to storage. */
            data class ConfigUpdateFailed(val cause: Throwable) : Failure
        }
    }
}
