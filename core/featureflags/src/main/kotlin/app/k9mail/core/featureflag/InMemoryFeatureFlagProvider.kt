package app.k9mail.core.featureflag

class InMemoryFeatureFlagProvider(
    featureFlagFactory: FeatureFlagFactory,
) : FeatureFlagProvider {

    private val features: Map<FeatureFlagKey, FeatureFlag> =
        featureFlagFactory.createFeatureCatalog().associateBy { it.key }

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        return when (features[key]?.enabled) {
            null -> FeatureFlagResult.Unavailable
            true -> FeatureFlagResult.Enabled
            false -> FeatureFlagResult.Disabled
        }
    }
}
