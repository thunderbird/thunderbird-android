package net.thunderbird.core.configstore

internal class TestConfigDefinition(
    override val mapper: TestConfigMapper,
    override val defaultValue: TestConfig,
    override val version: Int = 1,
    override val migration: ConfigMigration = object : ConfigMigration {
        override suspend fun migrate(currentVersion: Int, newVersion: Int, current: Config): ConfigMigrationResult {
            return ConfigMigrationResult.NoOp
        }
    },
) : ConfigDefinition<TestConfig> {
    override val id: ConfigId = ConfigId("test_backend", "test_feature")
    override val keys: List<ConfigKey<*>> = listOf(
        mapper.stringKey,
        mapper.intKey,
    )
}
