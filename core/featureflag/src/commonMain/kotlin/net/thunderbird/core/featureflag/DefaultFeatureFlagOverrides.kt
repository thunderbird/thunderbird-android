package net.thunderbird.core.featureflag

import kotlin.collections.plus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DefaultFeatureFlagOverrides(
    initialOverrides: Map<FeatureFlagKey, Boolean> = emptyMap(),
) : FeatureFlagOverrides {
    private val _overrides = MutableStateFlow(initialOverrides)
    override val overrides: StateFlow<Map<FeatureFlagKey, Boolean>> = _overrides.asStateFlow()

    override fun get(key: FeatureFlagKey): Boolean? = overrides.value[key]

    override fun set(key: FeatureFlagKey, value: Boolean) {
        _overrides.update { it + (key to value) }
    }

    override fun clear(key: FeatureFlagKey) {
        _overrides.update { it - key }
    }

    override fun clearAll() {
        _overrides.update { emptyMap() }
    }
}
