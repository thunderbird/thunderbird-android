package net.thunderbird.core.featureflag

/**
 * Represents a unique identifier for a feature flag with an optional description.
 *
 * Typically implemented as an enum or sealed class to provide a type-safe catalog
 * of all available feature flags in the application.
 */
interface FeatureFlagKey {
    /**
     * The unique identifier or name associated with this instance.
     */
    val key: String

    /**
     * Returns a human-readable description of this element, or null if no description is available.
     */
    val description: String? get() = null
}
