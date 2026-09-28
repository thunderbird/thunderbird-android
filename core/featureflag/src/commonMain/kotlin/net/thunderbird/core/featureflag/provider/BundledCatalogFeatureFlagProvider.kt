package net.thunderbird.core.featureflag.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.logging.Logger

/**
 * [FeatureFlagProvider][BaseCatalogFeatureFlagProvider] backed by the bundled (offline) catalog.
 *
 * The catalog is the offline floor of the evaluation chain and also exposes the
 * resolved baseline via [BundledFeatureFlagDefaults] for the debug settings UI.
 */
class BundledCatalogFeatureFlagProvider(
    dataSource: FeatureFlagCatalogDataSource,
    logger: Logger,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : DataSourceCatalogFeatureFlagProvider(
    dataSource = dataSource,
    providerName = "bundled_catalog",
    logger = logger,
    scope = scope,
),
    BundledFeatureFlagDefaults {
    override fun defaults(): Map<String, Boolean> = resolvedFlags()

    override fun toString(): String {
        return """
            |feature-flag provider '${metadata.name}':
            |   resolvedFlags = $resolvedFlags,
            |   defaults = ${defaults()}
        """.trimMargin()
    }
}

/**
 * Exposes the variant-resolved baseline value of every catalog flag (before any runtime debug
 * override), keyed by the flag key.
 *
 * Intended for surfaces such as the debug settings screen that need to enumerate all known flags
 * and their default state without going through the override-aware evaluation stack.
 */
fun interface BundledFeatureFlagDefaults {
    fun defaults(): Map<String, Boolean>
}
