package net.thunderbird.core.featureflag.model

/**
 * A singleton implementation of AppVariantOverrides that contains no feature flag overrides.
 *
 * This object is used to suppress any possible configuration that comes from an app that isn't the
 * one the user is currently using. All variant-specific queries will return empty results.
 */
data object EmptyAppVariantOverride : BaseAppVariantOverrides(wrapper = emptyMap()), AppVariantOverrides.Factory {
    override fun create(wrapper: Map<String, FlagOverrides>): AppVariantOverrides = this
}
