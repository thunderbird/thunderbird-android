package net.thunderbird.core.configstore

internal class TestConfigMapper : ConfigMapper<TestConfig> {
    val stringKey = ConfigKey.StringKey("string_key")
    val intKey = ConfigKey.IntKey("int_key")

    override fun toConfig(obj: TestConfig): Config {
        return Config().apply {
            this[stringKey] = obj.stringValue
            this[intKey] = obj.intValue
        }
    }

    override fun fromConfig(config: Config): TestConfig? {
        val stringValue = config[stringKey] ?: return null
        val intValue = config[intKey] ?: return null

        return TestConfig(
            stringValue = stringValue,
            intValue = intValue,
        )
    }
}
