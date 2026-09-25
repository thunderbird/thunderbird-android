package net.thunderbird.core.featureflag.provider

import java.io.IOException
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.logging.Logger

/**
 * Base implementation of a catalog-based feature flag provider that loads flag
 * definitions from a data source.
 *
 * @param dataSource The data source from which to load the feature flag catalog.
 * @param providerName The identifying name for this provider instance.
 * @param logger Logger instance for diagnostic and error messages.
 */
abstract class DataSourceCatalogFeatureFlagProvider internal constructor(
    private val dataSource: FeatureFlagCatalogDataSource,
    providerName: String,
    private val logger: Logger,
) : BaseCatalogFeatureFlagProvider(providerName, logger) {
    /**
     * Initializes the feature flag provider with the given context and loads the catalog.
     *
     * @param initialContext The evaluation context containing targeting key and attributes for flag resolution.
     */
    override suspend fun initialize(initialContext: FeatureFlagContext) {
        super.initialize(initialContext)
        try {
            catalog = loadCatalog()
            resolvedFlags = resolve(context)
            logger.verbose { "[feature-flag] Resolved feature flags: $resolvedFlags" }
        } catch (e: IOException) {
            logger.error(throwable = e) { "[feature-flag] Failed to load feature flag catalog." }
        }
    }

    /**
     * Loads the catalog as a [Flow], to resolve. Defaults to [FeatureFlagCatalogDataSource.load].
     *
     * @return A Flow that emits the feature flag catalog containing flag definitions and overrides.
     */
    open suspend fun loadCatalog(): FeatureFlagCatalog? = dataSource.load()
}
