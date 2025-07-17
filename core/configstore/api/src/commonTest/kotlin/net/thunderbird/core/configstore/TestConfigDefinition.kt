package net.thunderbird.core.configstore

internal class TestConfigDefinition(
    override val mapper: TestConfigMapper,
    override val defaultValue: TestConfig,
) : ConfigDefinition<TestConfig> {
    override val id: ConfigId = ConfigId("test_id")
    override val keys: List<ConfigKey<*>> = listOf(
        mapper.stringKey,
        mapper.intKey,
    )
}
