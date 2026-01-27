package net.thunderbird.core.featureflag

import kotlinx.coroutines.flow.Flow

/**
 * Creates a catalog of all available feature flags.
 *
 * This is a functional interface that can be implemented to provide a list of all feature flags
 * defined within an application or module.
 */
fun interface FeatureFlagFactory {
    /**
     * Creates and returns a reactive stream of the feature flag catalog.
     *
     * This function is responsible for assembling the complete catalog of [FeatureFlag]s
     * that the application recognizes. The returned [Flow] will emit a new list
     * whenever the state of any feature flag changes.
     *
     * @return A [Flow] that emits a [List] of [FeatureFlag] objects representing the
     * current state of all features.
     */
    fun getCatalog(): Flow<List<FeatureFlag>>
}
