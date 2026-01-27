package net.thunderbird.core.featureflag

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InMemoryFeatureFlagProvider(
    featureFlagFactory: FeatureFlagFactory,
    featureFlagOverrides: FeatureFlagOverrides,
    private val mainImmediateDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + mainImmediateDispatcher),
) : FeatureFlagProvider {
    private val combinedFeatureFlags = combine(
        featureFlagFactory.getCatalog(),
        featureFlagOverrides.overrides,
    ) { defaults, overrides ->
        val defaults = defaults.associateBy { it.key }
        val overrides = overrides.mapValues {
            FeatureFlag(
                key = it.key,
                enabled = it.value,
            )
        }
        defaults + overrides
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap(),
    )

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        return when (combinedFeatureFlags.value[key]?.enabled) {
            null -> FeatureFlagResult.Unavailable
            true -> FeatureFlagResult.Enabled
            false -> FeatureFlagResult.Disabled
        }
    }
}
