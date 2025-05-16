package net.thunderbird.core.featureflag

fun interface FeatureFlagProvider {
    fun provide(key: FeatureFlagKey): FeatureFlagResult
}
