package net.thunderbird.core.featureflag.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.logging.Logger

class RemoteCatalogFeatureFlagProvider(
    configStore: FeatureFlagConfigStore,
    dataSource: FeatureFlagCatalogDataSource,
    logger: Logger,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate),
) : DataSourceCatalogFeatureFlagProvider(
    dataSource = dataSource,
    providerName = "remote_catalog",
    logger = logger,
) {

    private val config = configStore
        .config
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    init {
        config
            .mapNotNull { it?.remoteCatalogConfig?.enabled }
            .distinctUntilChanged()
            .combine(catalog) { enabled, catalog ->
                val context = context
                when {
                    enabled && catalog == null && context != null -> {
                        logger.info { "$logPrefix Remote catalog is enabled. Loading remote catalog." }
                        initialize(context)
                    }

                    !enabled && catalog != null && context != null -> {
                        logger.info { "$logPrefix Remote catalog is disabled. Clearing remote catalog." }
                        clearCatalog()
                    }
                }
            }
            .launchIn(scope)
    }

    override suspend fun loadCatalog(): FeatureFlagCatalog? {
        val config = config.value
        if (config?.remoteCatalogConfig?.enabled == false) {
            logger.info { "$logPrefix Remote catalog is disabled. Skipping remote catalog load." }
            return null
        }
        val catalog = dataSource.load()
        if (catalog == null) {
            logger.warn { "$logPrefix Remote catalog is not available." }
        }
        return catalog
    }

    override fun toString(): String = """
        |feature-flag provider '${metadata.name}':
        |   resolvedFlags = ${resolvedFlags()},
    """.trimMargin()
}
