package net.thunderbird.core.featureflag.provider

import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
private const val TAG = "RemoteCatalogFeatureFlagProvider"

class RemoteCatalogFeatureFlagProvider(
    dataSource: FeatureFlagCatalogDataSource,
    logger: Logger,
) : DataSourceCatalogFeatureFlagProvider(
    dataSource = dataSource,
    providerName = "remote_catalog",
    logger = logger,
) {

    override suspend fun loadCatalog(): FeatureFlagCatalog? {
        val catalog = dataSource.load()
        if (catalog == null) {
            logger.warn(TAG) { "$logPrefix Remote catalog is not available." }
        }
        return catalog
    }

    override fun toString(): String = """
        |feature-flag provider '${metadata.name}':
        |   resolvedFlags = $resolvedFlags,
    """.trimMargin()
}
