package app.k9mail.core.featureflag

fun interface FeatureFlagProvider {
    fun provide(key: FeatureFlagKey): FeatureFlagResult
}
