package net.thunderbird.core.featureflag.provider

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.logging.Logger

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
            logger.warn { "$logPrefix Remote catalog is not available." }
        }
        return catalog
    }

    override fun toString(): String = """
        |feature-flag provider '${metadata.name}':
        |   resolvedFlags = $resolvedFlags,
    """.trimMargin()
}
