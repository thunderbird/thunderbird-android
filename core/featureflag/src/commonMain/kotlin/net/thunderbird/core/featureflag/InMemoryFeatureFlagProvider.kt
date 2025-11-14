package net.thunderbird.core.featureflag

class InMemoryFeatureFlagProvider(
    private val featureFlagFactory: MutableFeatureFlagFactory,
) : FeatureFlagProvider {

    private val features: Map<FeatureFlagKey, FeatureFlag>
        get() = featureFlagFactory.createFeatureCatalog().associateBy { it.key }

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        return when (features[key]?.enabled) {
            null -> FeatureFlagResult.Unavailable
            true -> FeatureFlagResult.Enabled
            false -> FeatureFlagResult.Disabled
        }
    }
}
