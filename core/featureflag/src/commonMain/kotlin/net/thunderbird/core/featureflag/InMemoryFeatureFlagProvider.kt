package net.thunderbird.core.featureflag

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class InMemoryFeatureFlagProvider(
    featureFlagFactory: FeatureFlagFactory,
    featureFlagOverrides: FeatureFlagOverrides,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + mainDispatcher),
) : FeatureFlagProvider {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val featureFlags = featureFlagOverrides
        .overrides
        .combine(flowOf(featureFlagFactory.createFeatureCatalog().associateBy { it.key })) { overrides, defaults ->
            overrides.map {
                it.key to FeatureFlag(
                    key = it.key,
                    enabled = it.value,
                )
            }.toMap() + defaults.filter { it.key !in overrides }
        }
        .stateIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = emptyMap(),
        )

    private val features: Map<FeatureFlagKey, FeatureFlag>
        get() = featureFlags.value

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        return when (features[key]?.enabled) {
            null -> FeatureFlagResult.Unavailable
            true -> FeatureFlagResult.Enabled
            false -> FeatureFlagResult.Disabled
        }
    }
}
