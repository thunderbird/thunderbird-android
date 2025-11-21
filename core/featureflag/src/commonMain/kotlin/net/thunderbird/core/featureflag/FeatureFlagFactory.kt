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
