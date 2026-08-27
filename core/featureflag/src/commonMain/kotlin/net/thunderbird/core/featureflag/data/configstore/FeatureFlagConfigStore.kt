package net.thunderbird.core.featureflag.data.configstore

import kotlinx.serialization.json.Json
import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigDefinition
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.core.configstore.ConfigMigration
import net.thunderbird.core.configstore.ConfigMigrationResult
import net.thunderbird.core.configstore.ConfigStore
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.featureflag.data.configstore.mapper.FeatureFlagConfigDefinitionMapper

/**
 * A configuration store specialized for managing feature flag configuration data.
 *
 * Extends ConfigStore to provide type-safe access to feature flag-specific configuration,
 * including targeting keys for rollout/experiment bucketing and flag overrides.
 */
interface FeatureFlagConfigStore : ConfigStore<FeatureFlagConfigData>

/**
 * Configuration store for managing feature flag-related configuration data.
 *
 * This store handles persistence and retrieval of feature flag configuration,
 * including the targeting key used for feature flag evaluation. It provides
 * type-safe access to feature flag settings through the [FeatureFlagConfigData] model.
 *
 * @param provider The backend provider used to access the underlying storage mechanism.
 */
internal class DefaultFeatureFlagConfigStore(
    id: ConfigId,
    provider: ConfigBackendProvider,
) : BaseConfigStore<FeatureFlagConfigData>(provider, FeatureFlagConfigDefinition(id)), FeatureFlagConfigStore

private class FeatureFlagConfigDefinition(override val id: ConfigId) : ConfigDefinition<FeatureFlagConfigData> {
    override val version: Int = 1
    override val defaultValue: FeatureFlagConfigData = FeatureFlagConfigData.DEFAULT
    override val keys: List<ConfigKey<*>> = listOf(
        FeatureFlagConfigKeys.TARGETING_KEY,
        FeatureFlagConfigKeys.OVERRIDES,
    )
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val mapper: ConfigMapper<FeatureFlagConfigData> = FeatureFlagConfigDefinitionMapper(json)

    override val migration: ConfigMigration = object : ConfigMigration {
        override suspend fun migrate(
            currentVersion: Int,
            newVersion: Int,
            current: Config,
        ): ConfigMigrationResult = ConfigMigrationResult.NoOp
    }
}
