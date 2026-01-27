package net.thunderbird.core.featureflag

import kotlinx.coroutines.flow.StateFlow

/**
 * Defines a contract for managing local overrides of feature flags.
 *
 * This is primarily intended for debugging and development purposes, allowing developers
 * to force a feature flag to be enabled or disabled, overriding its default or remote value.
 * Implementations should provide a mechanism to store and retrieve these overrides.
 */
interface FeatureFlagOverrides {
    /**
     * A [StateFlow] emitting the current map of feature flag overrides.
     *
     * This flow allows observers to react to changes in the override values, for example,
     * to update the UI when a feature flag is toggled in a debug menu.
     *
     * The map keys are [FeatureFlagKey]s and the values are booleans representing the
     * overridden state (true for enabled, false for disabled).
     */
    val overrides: StateFlow<Map<FeatureFlagKey, Boolean>>

    /**
     * Returns the override value for a given [key], or `null` if no override is set.
     *
     * This allows for retrieving an override using the index access operator (`[]`).
     *
     * Example:
     * ```
     * val overrideValue = featureFlagOverrides[FeatureFlagKey.SomeFeature]
     * ```
     *
     * @param key The [FeatureFlagKey] for the feature flag to check.
     * @return The [Boolean] override value, or `null` if it's not overridden.
     */
    operator fun get(key: FeatureFlagKey): Boolean?

    /**
     * Sets an override for a feature flag.
     *
     * This will cause the feature flag identified by [key] to return the specified [value],
     * regardless of its default state or other configurations.
     *
     * @param key The unique key for the feature flag to override.
     * @param value The boolean value to force the feature flag to have.
     */
    operator fun set(key: FeatureFlagKey, value: Boolean)

    /**
     * Removes the override for the given feature flag [key].
     *
     * After this is called, the feature flag will revert to its default value.
     *
     * @param key The [FeatureFlagKey] of the override to remove.
     */
    fun clear(key: FeatureFlagKey)

    /**
     * Removes all feature flag overrides.
     */
    fun clearAll()
}
