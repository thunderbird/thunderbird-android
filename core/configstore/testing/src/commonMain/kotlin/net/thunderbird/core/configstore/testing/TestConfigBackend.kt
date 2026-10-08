package net.thunderbird.core.configstore.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.backend.ConfigBackend

/**
 * A mutable, in-memory implementation of [ConfigBackend] designed for testing.
 *
 * This implementation allows tests to simulate configuration storage without relying on
 * actual persistence mechanisms like DataStore. It stores configuration in a [MutableStateFlow],
 * providing reactive updates and thread-safe access.
 *
 * Use this in unit tests where you need to verify how components interact with the config store
 * or to provide a controlled environment for configuration-dependent logic.
 *
 * @param initialConfig The starting configuration state. Defaults to an empty [Config].
 */
class TestConfigBackend(
    initialConfig: Config = Config(),
) : ConfigBackend {

    private val config: MutableStateFlow<Config> = MutableStateFlow(initialConfig)

    override fun read(keys: List<ConfigKey<*>>): Flow<Config> = config.asStateFlow()

    override suspend fun update(
        keys: List<ConfigKey<*>>,
        transform: (Config) -> Config,
    ) {
        config.update { transform(it) }
    }

    override suspend fun clear() {
        config.value = Config()
    }

    override suspend fun readVersion(versionKey: String): Int {
        return config.value[ConfigKey.IntKey(versionKey)] ?: 0
    }

    override suspend fun writeVersion(versionKey: String, version: Int) {
        config.update { current ->
            val newConfig = current.copy()
            newConfig[ConfigKey.IntKey(versionKey)] = version
            newConfig
        }
    }

    override suspend fun removeKeys(keys: Set<ConfigKey<*>>) {
        config.update { current ->
            val newMap = current.toMap().toMutableMap()
            keys.forEach { newMap.remove(it) }
            Config(newMap)
        }
    }
}
