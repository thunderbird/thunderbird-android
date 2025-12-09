package net.thunderbird.core.featureflag

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NoOpFeatureFlagOverrides : FeatureFlagOverrides {
    override val overrides: StateFlow<Map<FeatureFlagKey, Boolean>> =
        MutableStateFlow(emptyMap<FeatureFlagKey, Boolean>()).asStateFlow()

    override fun get(key: FeatureFlagKey): Boolean? = false

    override fun set(key: FeatureFlagKey, value: Boolean) = Unit

    override fun clear(key: FeatureFlagKey) = Unit

    override fun clearAll() = Unit
}
