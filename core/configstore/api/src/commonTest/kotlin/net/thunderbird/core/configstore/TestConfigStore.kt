package net.thunderbird.core.configstore

import net.thunderbird.core.configstore.backend.ConfigBackendProvider

internal class TestConfigStore(
    provider: ConfigBackendProvider,
    definition: ConfigDefinition<TestConfig>,
) : BaseConfigStore<TestConfig>(provider, definition)
