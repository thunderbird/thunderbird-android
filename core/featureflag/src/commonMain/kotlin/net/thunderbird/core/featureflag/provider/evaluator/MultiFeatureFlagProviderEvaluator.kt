package net.thunderbird.core.featureflag.provider.evaluator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.featureflag.provider.BaseCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.BundledCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.logging.Logger

/**
 * Feature flag provider that coordinates multiple catalog providers and supports initialization with context.
 */
interface MultiFeatureFlagProviderEvaluator : CatalogFeatureFlagProvider {
    val enabledFlags: StateFlow<List<FeatureFlagKey>>

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
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : BaseCatalogFeatureFlagProvider(
    providerName = "multi_provider",
    logger = logger,
    scope = scope,
),
    MultiFeatureFlagProviderEvaluator {

    init {
        scope.launch {
            combine(
                flows = providers.map { provider -> provider.state.map { provider.metadata.name to it } },
            ) { providerStates -> providerStates }
                .collect { providerStates ->
                    var resolved = 0
                    for ((provider, state) in providerStates) {
                        logger.verbose { "$logPrefix provider '$provider' state: $state" }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override val enabledFlags: StateFlow<List<FeatureFlagKey>> = combine(
        providers.filterIsInstance<BaseCatalogFeatureFlagProvider>().map { it.resolvedFlags },
    ) { flags ->
        // Providers are ordered from highest to lowest priority, so merge them in reverse to let
        // higher priority values (including `false`) win, matching the first-match order of [provide].
        flags
            .reversed()
            .fold(emptyMap<String, Boolean>()) { merged, providerFlags -> merged + providerFlags }
            .filterValues { it }
    }
        .onEach { logger.verbose { "[feature-flag][1] provider: $it" } }
        .map { enabledFlagOverrides ->
            enabledFlagOverrides.keys.mapNotNull { key -> GeneratedFeatureFlagKey.entries.find { it.key == key } }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10000L),
            initialValue = emptyList(),
        )

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

    override suspend fun initialize(initialContext: FeatureFlagContext) {
        super.initialize(initialContext)
        val bundledCatalogProvider =
            checkNotNull(providers.filterIsInstance<BundledCatalogFeatureFlagProvider>().singleOrNull()) {
                "$logPrefix A MultiFeatureFlagProviderEvaluator requires a one BundledCatalogFeatureFlagProvider"
            }

        bundledCatalogProvider.initialize(initialContext)

        providers
            .filterNot { it == bundledCatalogProvider }
            .filterIsInstance<BaseCatalogFeatureFlagProvider>()
            .forEach { it.initialize(initialContext) }
    }

    override fun toString(): String = "feature-flag provider '${metadata.name}': ${providers.size} providers"
}
