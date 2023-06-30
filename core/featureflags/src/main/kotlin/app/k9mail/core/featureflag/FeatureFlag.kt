package app.k9mail.core.featureflag

data class FeatureFlag(
    val key: FeatureFlagKey,
    val enabled: Boolean = false,
)
