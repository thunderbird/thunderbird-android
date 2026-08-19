package net.thunderbird.core.featureflag.provider.evaluator

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
    fun initialize(initialContext: FeatureFlagContext)
}

internal class DefaultMultiFeatureFlagProviderEvaluator(
    private val providers: List<CatalogFeatureFlagProvider>,
    private val logger: Logger,
) : BaseCatalogFeatureFlagProvider(
    providerName = "multi_provider",
    logger = logger,
),
    MultiFeatureFlagProviderEvaluator {

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

    override fun initialize(initialContext: FeatureFlagContext) {
        super.initialize(initialContext)
        val bundledCatalogProvider =
            checkNotNull(providers.filterIsInstance<DataSourceCatalogFeatureFlagProvider>().singleOrNull()) {
                "[feature-flag] A MultiFeatureFlagProviderEvaluator requires a one CatalogFeatureFlagProvider"
            }

        bundledCatalogProvider.initialize(initialContext)
    }

    override fun toString(): String = "feature-flag provider '${metadata.name}': ${providers.size} providers"
}
