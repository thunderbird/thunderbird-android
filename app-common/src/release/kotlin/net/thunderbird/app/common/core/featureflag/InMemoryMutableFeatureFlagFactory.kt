package net.thunderbird.app.common.core.featureflag

import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.MutableFeatureFlagFactory

class InMemoryMutableFeatureFlagFactory(
    private val featureFlagFactory: FeatureFlagFactory,
) : MutableFeatureFlagFactory {
    override val defaults: List<FeatureFlag> = featureFlagFactory.createFeatureCatalog()
    override val overrides: Map<FeatureFlagKey, Boolean> = emptyMap()
    override fun override(key: FeatureFlagKey, enabled: Boolean) = Unit
    override fun restoreDefaults() = Unit

    override fun createFeatureCatalog(): List<FeatureFlag> =
        featureFlagFactory.createFeatureCatalog()
}
