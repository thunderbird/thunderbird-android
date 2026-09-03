package net.thunderbird.core.featureflag.data.configstore

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigStore
import net.thunderbird.core.featureflag.model.FlagOverrides

/**
 * Data class representing the configuration for feature flags.
 *
 * @property targetingKey A random UUID used only for rollout/experiment bucketing — never PII.
 *  A `null` value means no key has been generated yet.
 */
data class FeatureFlagConfigData(
    val targetingKey: Uuid? = null,
    val overrides: FlagOverrides = emptyMap(),
) {
    companion object {
        val DEFAULT = FeatureFlagConfigData()
    }
}

internal object FeatureFlagConfigKeys {
    val TARGETING_KEY = ConfigKey.StringKey("targeting_key")

    /** JSON-encoded [FlagOverrides], since the backend only supports scalar values. */
    val OVERRIDES = ConfigKey.StringKey("overrides")
}

/**
 * Updates the stored configuration by applying the provided transformation function
 *
 * @param transform A function that takes the current configuration, or default if null,
 * and returns a new configuration.
 */
suspend fun ConfigStore<FeatureFlagConfigData>.update(transform: (FeatureFlagConfigData) -> FeatureFlagConfigData) =
    update { nullableConfig ->
        val config = nullableConfig ?: FeatureFlagConfigData.DEFAULT
        transform(config)
    }

/** Returns the persisted per-install targeting key, generating and persisting one on first use. */
internal suspend fun ConfigStore<FeatureFlagConfigData>.resolveTargetingKey(): Uuid {
    val current = config.first().targetingKey ?: Uuid.random().also { generated ->
        update {
            val config = requireNotNull(it) {
                "Feature flag configuration data was not properly initialized."
            }
            config.copy(targetingKey = generated)
        }
    }
    return current
}
