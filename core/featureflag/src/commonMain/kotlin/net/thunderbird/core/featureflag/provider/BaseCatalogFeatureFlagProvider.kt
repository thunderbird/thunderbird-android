package net.thunderbird.core.featureflag.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.logging.Logger

/**
 *
 * Base [FeatureFlagCatalog] for a feature-flag catalog loaded through a [FeatureFlagCatalogDataSource].
 *
 * Effective flag values are the catalog [base defaults][FeatureFlagCatalog.flags] overlaid with the
 * per-build-type overrides for the current build (`app`/`build_type` attributes from the
 * [FeatureFlagContext]; overrides win), re-resolved whenever the context changes. Keys absent from
 * the resolved catalog return [FeatureFlagResult.Unavailable], so a `MultiProvider` using first-match
 * strategy falls through to the next provider.
 *
 * @param dataSource The data source from which to load the feature flag catalog.
 * @param providerName The identifying name for this provider instance.
 * @param logger Logger instance for diagnostic and error messages.
 * @param scope Coroutine scope for managing asynchronous catalog loading operations.
 */
abstract class BaseCatalogFeatureFlagProvider(
    private val dataSource: FeatureFlagCatalogDataSource,
    providerName: String,
    private val logger: Logger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : FeatureFlagProvider {

    private var catalog: FeatureFlagCatalog? = null
    private var resolvedFlags: Map<String, Boolean> = emptyMap()
    private var context: FeatureFlagContext? = null

    val metadata: ProviderMetadata = CatalogProviderMetadata(providerName)

    /**
     * Initializes the feature flag provider with the given context and loads the catalog.
     *
     * @param initialContext The evaluation context containing targeting key and attributes for flag resolution.
     */
    suspend fun initialize(initialContext: FeatureFlagContext) {
        context = initialContext
        loadCatalog()
            .onEach { bundledCatalog ->
                catalog = bundledCatalog
                resolvedFlags = resolve(context)
                logger.verbose { "Resolved feature flags: $resolvedFlags" }
            }
            .catch { cause ->
                logger.error(throwable = cause) { "Failed to load feature flag catalog." }
            }
            .launchIn(scope)
    }

    override fun provide(key: FeatureFlagKey): FeatureFlagResult = when (resolvedFlags[key.key]) {
        null -> FeatureFlagResult.Unavailable
        true -> FeatureFlagResult.Enabled
        false -> FeatureFlagResult.Disabled
    }

    /**
     * Loads the catalog as a [Flow], to resolve. Defaults to [FeatureFlagCatalogDataSource.load].
     *
     * @return A Flow that emits the feature flag catalog containing flag definitions and overrides.
     */
    protected open suspend fun loadCatalog(): Flow<FeatureFlagCatalog> = dataSource.load()

    /** The variant-resolved baseline values, for surfaces that enumerate all flags. */
    protected fun resolvedFlags(): Map<String, Boolean> = resolvedFlags

    private fun resolve(context: FeatureFlagContext?): Map<String, Boolean> {
        val catalog = catalog ?: return emptyMap()
        val base = catalog.flags.associate { it.key to it.default }

        val app = context?.get(key = "app")?.asString()
        val buildType = context?.get(key = "build_type")?.asString()
        val overrides = if (app != null && buildType != null) {
            catalog.overrides[app]?.get(buildType).orEmpty()
        } else {
            emptyMap()
        }

        return base + overrides
    }

    override fun toString(): String {
        return "BaseCatalogFeatureFlagProvider('${metadata.name}', resolvedFlags=$resolvedFlags)"
    }
}
