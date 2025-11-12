package net.thunderbird.app.common.core.featureflag

import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.MutableFeatureFlagFactory

class InMemoryMutableFeatureFlagFactory(
    private val featureFlagFactory: FeatureFlagFactory,
) : MutableFeatureFlagFactory {
    override val defaults: List<FeatureFlag>
        get() = featureFlagFactory.createFeatureCatalog()
    override val overrides = mutableMapOf<FeatureFlagKey, Boolean>()

    override fun override(key: FeatureFlagKey, enabled: Boolean) {
        overrides[key] = enabled
    }

    override fun restoreDefaults() {
        overrides.clear()
    }

    override fun createFeatureCatalog(): List<FeatureFlag> =
        defaults.map { flag ->
            overrides[flag.key]?.let { flag.copy(enabled = it) } ?: flag
        }
}
