package net.thunderbird.android.featureflag

import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FlagOverrides

/**
 * Thunderbird-specific implementation of application variant overrides for feature flags.
 *
 * Extends the base variant overrides (debug, release) with Thunderbird-specific build variants
 * for daily and beta channels. This allows feature flags to be configured differently across
 * all Thunderbird build variants.
 *
 * @param wrapper Map of variant names to their corresponding flag override configurations.
 */
class ThunderbirdOverrides(wrapper: Map<String, FlagOverrides>) : BaseAppVariantOverrides(wrapper) {
    /** Feature flag overrides for the Thunderbird daily build variant. */
    val daily by wrapper

    /** Feature flag overrides for the Thunderbird beta build variant. */
    val beta by wrapper

    companion object {
        val Factory = AppVariantOverrides.Factory { wrapper -> ThunderbirdOverrides(wrapper) }
    }
}
