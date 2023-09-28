package app.k9mail.core.featureflag

@JvmInline
value class FeatureFlagKey(val key: String)

fun String.toFeatureFlagKey(): FeatureFlagKey = FeatureFlagKey(this)
