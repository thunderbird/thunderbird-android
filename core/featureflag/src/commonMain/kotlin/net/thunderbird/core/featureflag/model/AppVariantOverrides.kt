package net.thunderbird.core.featureflag.model

/**
 * Represents build variant-specific feature flag overrides for an application.
 *
 * Provides a map-based interface to access feature flag overrides by build variant name,
 * with dedicated properties for debug and release configurations. This allows different
 * feature flag states to be applied depending on the build variant.
 */
interface AppVariantOverrides : AppVariantOverridesRawType {
    /** Feature flag overrides for the debug build variant. */
    val debug: FlagOverrides

    /** Feature flag overrides specific to release build variants. */
    val release: FlagOverrides

    fun interface Factory {
        fun create(wrapper: Map<String, FlagOverrides>): AppVariantOverrides
    }
}

internal typealias AppVariantOverridesRawType = Map<String, FlagOverrides>

/**
 * Maps feature flag keys to their boolean override values.
 *
 * @remarks In future implementation, the value will be changed to [Any].
 */
typealias FlagOverrides = Map<String, Boolean>

/**
 * Base implementation of [AppVariantOverrides] that provides common functionality
 * for build variant-specific feature flag overrides.
 *
 * @param wrapper Map containing variant names as keys and their corresponding FlagOverrides.
 */
abstract class BaseAppVariantOverrides(wrapper: AppVariantOverridesRawType) :
    AppVariantOverrides,
    AppVariantOverridesRawType by wrapper {
    override val debug: FlagOverrides by wrapper
    override val release: FlagOverrides by wrapper
}
