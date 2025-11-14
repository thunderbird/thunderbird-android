package net.thunderbird.core.featureflag

/**
 * Creates a catalog of all available feature flags.
 *
 * This is a functional interface that can be implemented to provide a list of all feature flags
 * defined within an application or module.
 */
fun interface FeatureFlagFactory {
    /**
     * Creates and returns a list of all available feature flags.
     *
     * This function is responsible for assembling the complete catalog of [FeatureFlag]s
     * that the application recognizes.
     *
     * @return A [List] of [FeatureFlag] objects representing the current state of all features.
     */
    fun createFeatureCatalog(): List<FeatureFlag>
}

/**
 * A mutable [FeatureFlagFactory] that allows for overriding feature flag values at runtime.
 *
 * This is primarily intended for use in development and testing environments to easily
 * enable or disable features without requiring a full application restart or code change.
 *
 * ```
 * !! IMPORTANT: DO NOT INJECT IN FEATURES THAT ARE AVAILABLE ON RELEASE MODE. !!
 * !!            INJECTION AVAILABLE ONLY ON DEBUG MODE.                       !!
 * ```
 * @property defaults The default set of feature flags.
 * @property overrides A map of feature flag keys to their overridden boolean values.
 */
interface MutableFeatureFlagFactory : FeatureFlagFactory {
    /**
     * The default set of feature flags.
     *
     * This list represents the original, hardcoded state of all feature flags before any
     * runtime overrides are applied. It can be used to restore the feature flag state
     * to its default configuration.
     */
    val defaults: List<FeatureFlag>

    /**
     * A map of feature flags that have been overridden.
     * The key is the [FeatureFlagKey] and the value is the new `enabled` state.
     * These overrides take precedence over the [defaults].
     */
    val overrides: Map<FeatureFlagKey, Boolean>

    /**
     * Overrides the enabled state of a feature flag for the given [key].
     *
     * This change is temporary and will be reflected in the next call to [createFeatureCatalog].
     * The override can be reverted by calling [restoreDefaults].
     *
     * @param key The unique key of the feature flag to override.
     * @param enabled The new state for the feature flag (`true` for enabled, `false` for disabled).
     */
    fun override(key: FeatureFlagKey, enabled: Boolean)

    /**
     * Restores all feature flags to their default values by clearing any existing overrides.
     */
    fun restoreDefaults()
}
