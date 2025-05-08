package net.thunderbird.core.featureflag

data class FeatureFlag(
    val key: FeatureFlagKey,
    val enabled: Boolean = false,
)
