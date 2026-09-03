package net.thunderbird.core.featureflag.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.update
import net.thunderbird.core.featureflag.model.FlagOverrides
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.logging.Logger

/**
 * Runtime feature-flag provider that owns the debug overrides.
 */
class RuntimeDebugOverrideFeatureFlagProvider(
    private val configStore: FeatureFlagConfigStore,
    private val logger: Logger,
    // The dispatcher is inlined here on purpose: as a separate parameter its default would be
    // evaluated even when a scope is supplied, which throws wherever Dispatchers.Main is absent.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : BaseCatalogFeatureFlagProvider(
    providerName = "runtime_catalog",
    logger = logger,
) {
    /** The current debug overrides (flag key -> enabled), observable by the debug settings UI. */
    val data: StateFlow<FeatureFlagConfigData> = configStore
        .config
        .onEach { data ->
            logger.verbose { "[feature-flag] runtime override data update: $data" }
            // Keeps flag evaluation in sync with the persisted overrides, so a toggle takes effect
            // without recreating the provider.
            resolvedFlags = data.overrides
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = FeatureFlagConfigData.DEFAULT,
        )

    override var resolvedFlags: FlagOverrides = data.value.overrides
    val overrides: Flow<FlagOverrides> = data.map { it.overrides }

    override suspend fun initialize(initialContext: FeatureFlagContext) {
        super.initialize(initialContext)
        updateState { CatalogFeatureFlagProvider.State.Resolved }
    }

    /** Sets the override for [key] to [enabled] and persists it. */
    suspend fun setOverride(key: FeatureFlagKey, enabled: Boolean) {
        val key = key.key
        logger.verbose { "[feature-flag] overriding '$key' with '$enabled' value" }
        configStore.update { current: FeatureFlagConfigData ->
            current.copy(overrides = current.overrides + (key to enabled))
        }
    }

    /** Removes the override for [key] and persists the change. */
    suspend fun clearOverride(key: FeatureFlagKey) {
        val key = key.key
        logger.verbose { "[feature-flag] clearing '$key' override" }
        configStore.update { current: FeatureFlagConfigData ->
            current.copy(overrides = current.overrides - key)
        }
    }

    /** Removes all overrides and persists the change. */
    suspend fun clearAllOverrides() {
        logger.verbose { "[feature-flag] clearing all flag overrides" }
        // Only the overrides are dropped; clearing the whole store would also discard the
        // per-install targeting key used for rollout bucketing.
        configStore.update { current: FeatureFlagConfigData ->
            current.copy(overrides = emptyMap())
        }
    }

    override fun toString(): String {
        return """
            |feature-flag provider '${metadata.name}':
            |   resolvedFlags = $resolvedFlags,
        """.trimMargin()
    }
}
