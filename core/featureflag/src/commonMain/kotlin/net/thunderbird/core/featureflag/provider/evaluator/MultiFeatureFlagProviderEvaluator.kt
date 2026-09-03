package net.thunderbird.core.featureflag.provider.evaluator

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.provider.BaseCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.CatalogProviderMetadata
import net.thunderbird.core.featureflag.provider.DataSourceCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.ProviderMetadata
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.logging.Logger

/**
 * Feature flag provider that coordinates multiple catalog providers and supports initialization with context.
 */
interface MultiFeatureFlagProviderEvaluator : CatalogFeatureFlagProvider {

    /**
     * Initializes the feature flag provider with the given context and loads the catalog.
     *
     * @param initialContext The evaluation context containing targeting key and attributes for flag resolution.
     */
    suspend fun initialize(initialContext: FeatureFlagContext)
}

internal class DefaultMultiFeatureFlagProviderEvaluator(
    private val providers: List<CatalogFeatureFlagProvider>,
    private val logger: Logger,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : BaseCatalogFeatureFlagProvider(
    providerName = "multi_provider",
    logger = logger,
),
    MultiFeatureFlagProviderEvaluator {
    private val scope: CoroutineScope = CoroutineScope(mainDispatcher)

    init {
        scope.launch {
            combine(
                flows = providers.map { provider -> provider.state.map { provider.metadata.name to it } },
            ) { providerStates -> providerStates }
                .collect { providerStates ->
                    var resolved = 0
                    for ((provider, state) in providerStates) {
                        logger.verbose { "[feature-flag][${metadata.name}] provider '$provider' state: $state" }
                        if (state == CatalogFeatureFlagProvider.State.Resolved) {
                            resolved++
                        }
                    }

                    val isResolved = resolved == providers.size
                    if (isResolved) {
                        updateState { CatalogFeatureFlagProvider.State.Resolved }
                    }
                }
        }
    }

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        for (provider in providers) {
            val result = provider.provide(key)
            logger.verbose { "[feature-flag][${provider.metadata.name}] providing '${key.key}' -> $result" }
            if (result != FeatureFlagResult.Unavailable) {
                return result
            } else {
                logger.verbose { "[feature-flag][${provider.metadata.name}] fetching '${key.key}' on next provider" }
            }
        }
        return FeatureFlagResult.Unavailable
    }

    override val metadata: ProviderMetadata = CatalogProviderMetadata(name = "multi_provider")

    override suspend fun initialize(initialContext: FeatureFlagContext) {
        super.initialize(initialContext)
        val bundledCatalogProvider =
            checkNotNull(providers.filterIsInstance<DataSourceCatalogFeatureFlagProvider>().singleOrNull()) {
                "[feature-flag] A MultiFeatureFlagProviderEvaluator requires a one CatalogFeatureFlagProvider"
            }

        bundledCatalogProvider.initialize(initialContext)

        providers
            .filterNot { it == bundledCatalogProvider }
            .filterIsInstance<BaseCatalogFeatureFlagProvider>()
            .forEach { it.initialize(initialContext) }
    }

    override fun toString(): String = "feature-flag provider '${metadata.name}': ${providers.size} providers"
}
