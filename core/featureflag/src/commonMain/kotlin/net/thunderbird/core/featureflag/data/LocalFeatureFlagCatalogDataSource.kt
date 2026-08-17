package net.thunderbird.core.featureflag.data

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog

/**
 * Platform-specific implementation of [FeatureFlagCatalogDataSource] that loads the feature flag catalog
 * from local bundled resources.
 *
 * This is an expect class with platform-specific actual implementations that retrieve and parse
 * the catalog JSON from local application resources. The catalog is loaded asynchronously and
 * emitted as a Flow.
 */
internal expect class LocalFeatureFlagCatalogDataSource : FeatureFlagCatalogDataSource {
    override fun load(): Flow<FeatureFlagCatalog>
}
