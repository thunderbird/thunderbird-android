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
     * Implementations may trigger a [load] when collected. New subscribers receive the latest
     * catalog, or `null` while none could be loaded yet. A catalog that was loaded once may keep
     * being emitted; deciding whether it should still be applied (e.g. when the remote catalog is
     * disabled by the user) is up to the caller.
     *
     * @return A Flow of the most recently loaded feature flag catalog, or `null` if none is available.
     */
    fun observe(): Flow<FeatureFlagCatalog?>

    /**
     * Loads the feature flag catalog.
     *
     * Implementations may return a previously loaded catalog instead of loading it again. This
     * does not check whether the catalog is enabled; callers are expected to do so.
     *
     * @return The feature flag catalog containing flag definitions and overrides, or `null` if the
     *  catalog could not be loaded (e.g. a network, cache or parsing failure).
     */
    suspend fun load(): FeatureFlagCatalog?
}
