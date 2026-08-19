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
     * Loads the feature flag catalog as a reactive stream.
     *
     * @return A Flow that emits the feature flag catalog containing flag definitions and overrides.
     */
    fun load(): Flow<FeatureFlagCatalog>
}
