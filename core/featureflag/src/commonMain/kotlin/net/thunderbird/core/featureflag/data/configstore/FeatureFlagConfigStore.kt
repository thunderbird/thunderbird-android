package net.thunderbird.core.featureflag.data.configstore

import kotlin.uuid.Uuid
import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigDefinition
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.core.configstore.ConfigMigration
import net.thunderbird.core.configstore.ConfigMigrationResult
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

/**
 * Configuration store for managing feature flag-related configuration data.
 *
 * This store handles persistence and retrieval of feature flag configuration,
 * including the targeting key used for feature flag evaluation. It provides
 * type-safe access to feature flag settings through the [FeatureFlagConfigData] model.
 *
 * @param provider The backend provider used to access the underlying storage mechanism.
 */
class FeatureFlagConfigStore(
    id: ConfigId,
    provider: ConfigBackendProvider,
) : BaseConfigStore<FeatureFlagConfigData>(provider, FeatureFlagConfigDefinition(id))

private class FeatureFlagConfigDefinition(override val id: ConfigId) : ConfigDefinition<FeatureFlagConfigData> {
    override val version: Int = 1
    override val defaultValue: FeatureFlagConfigData = FeatureFlagConfigData.DEFAULT
    override val keys: List<ConfigKey<*>> = listOf(FeatureFlagConfigKeys.TARGETING_KEY)

    override val mapper: ConfigMapper<FeatureFlagConfigData> = object : ConfigMapper<FeatureFlagConfigData> {
        override fun toConfig(obj: FeatureFlagConfigData): Config = Config().apply {
            if (obj.targetingKey != null) {
                this[FeatureFlagConfigKeys.TARGETING_KEY] = obj.targetingKey.toString()
            }
        }

        override fun fromConfig(config: Config): FeatureFlagConfigData = FeatureFlagConfigData(
            targetingKey = config[FeatureFlagConfigKeys.TARGETING_KEY]?.let(Uuid::parse),
        )
    }

    override val migration: ConfigMigration = object : ConfigMigration {
        override suspend fun migrate(
            currentVersion: Int,
            newVersion: Int,
            current: Config,
        ): ConfigMigrationResult = ConfigMigrationResult.NoOp
    }
}
