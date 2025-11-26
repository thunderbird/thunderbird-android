package net.thunderbird.core.featureflag

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class InMemoryFeatureFlagProvider(
    featureFlagFactory: FeatureFlagFactory,
    featureFlagOverrides: FeatureFlagOverrides,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + mainDispatcher),
) : FeatureFlagProvider {
    private val featureFlags = featureFlagOverrides
        .overrides
        .combine(flowOf(featureFlagFactory.createFeatureCatalog().associateBy { it.key })) { overrides, defaults ->
            defaults + overrides.mapValues {
                FeatureFlag(
                    key = it.key,
                    enabled = it.value,
                )
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap(),
        )

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        return when (featureFlags.value[key]?.enabled) {
            null -> FeatureFlagResult.Unavailable
            true -> FeatureFlagResult.Enabled
            false -> FeatureFlagResult.Disabled
        }
    }
}
