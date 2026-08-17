package net.thunderbird.core.featureflag.provider

import androidx.annotation.CallSuper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagOverrides
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider.State
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.logging.Logger

/**
 * Extended interface for catalog-based feature flag providers.
 *
 * Provides feature flags loaded from a catalog data source while exposing
 * provider metadata for identification and debugging purposes. Implementations
 * include bundled catalogs (offline) and remote catalogs (fetched at runtime).
 */
interface CatalogFeatureFlagProvider : FeatureFlagProvider {
    val state: StateFlow<State>
    val metadata: ProviderMetadata

    override fun toString(): String

    enum class State { Initializing, ResolvingFlags, Resolved }
}

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
 * @param providerName The identifying name for this provider instance.
 * @param logger Logger instance for diagnostic and error messages.
 */
abstract class BaseCatalogFeatureFlagProvider internal constructor(
    providerName: String,
    private val logger: Logger,
) : CatalogFeatureFlagProvider {

    final override val state: StateFlow<State>
        field: MutableStateFlow<State> = MutableStateFlow(State.Initializing)

    protected var catalog: FeatureFlagCatalog? = null
    protected open var resolvedFlags: FlagOverrides = emptyMap()
    protected var context: FeatureFlagContext? = null

    override val metadata: ProviderMetadata = CatalogProviderMetadata(providerName)

    /**
     * Initializes the feature flag provider with the given context and loads the catalog.
     *
     * @param initialContext The evaluation context containing targeting key and attributes for flag resolution.
     */
    @CallSuper
    open fun initialize(initialContext: FeatureFlagContext) {
        context = initialContext
    }

    override fun provide(key: FeatureFlagKey): FeatureFlagResult = when (resolvedFlags[key.key]) {
        null -> FeatureFlagResult.Unavailable
        true -> FeatureFlagResult.Enabled
        false -> FeatureFlagResult.Disabled
    }

    /** The variant-resolved baseline values, for surfaces that enumerate all flags. */
    protected fun resolvedFlags(): Map<String, Boolean> = resolvedFlags

    protected fun resolve(context: FeatureFlagContext?): Map<String, Boolean> {
        state.update { State.ResolvingFlags }
        logger.verbose { "[feature-flag] resolving feature flag catalog for '${metadata.name}' provider" }
        val catalog = catalog ?: return emptyMap()
        val base = catalog.flags.associate { it.key to it.default }
        logger.verbose { "[feature-flag][${metadata.name}] base flags: $base" }

        val app = context?.get(key = "app")?.asString()
        val buildType = context?.get(key = "build_type")?.asString()
        logger.verbose { "[feature-flag][${metadata.name}] fetching overrides for '$app/$buildType'" }
        val overrides = if (app != null && buildType != null) {
            catalog.overrides[app]?.get(buildType).orEmpty()
        } else {
            emptyMap()
        }
        val resolvedFlags = base + overrides
        logger.verbose { "[feature-flag][${metadata.name}] resolved flags: $resolvedFlags" }
        state.update { State.Resolved }
        return resolvedFlags
    }
}
