package net.thunderbird.core.featureflag.data

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog

/**
 * Data source interface for loading feature flag catalog configurations.
 *
 * Implementations provide the mechanism to retrieve the complete feature flag catalog,
 * which includes flag definitions and application-specific overrides. The catalog can be
 * loaded from various sources such as local resources, remote servers, or other storage mechanisms.
 */
interface FeatureFlagCatalogDataSource {
    /**
     * Observes the most recently loaded feature flag catalog.
     *
     * Emits whenever [load] successfully retrieves a catalog, and replays the latest one to new
     * subscribers. Emits nothing until the first successful [load] call, and does not emit on failure.
     *
     * @return A Flow of the most recently loaded feature flag catalog.
     */
    fun observe(): Flow<FeatureFlagCatalog>

    /**
     * Loads the feature flag catalog.
     *
     * @return The feature flag catalog containing flag definitions and overrides, or `null` if the
     *  catalog could not be loaded (e.g. disabled by configuration, or a network/parsing failure).
     */
    suspend fun load(): FeatureFlagCatalog?
}
