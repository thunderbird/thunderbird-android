package net.thunderbird.core.configstore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.thunderbird.core.configstore.backend.ConfigBackend

internal class FakeConfigBackend : ConfigBackend {
    private val configFlow = MutableStateFlow(Config())
    private var lastStoredConfig: Config? = null
    var wasCleared = false

    fun setConfig(config: Config) {
        configFlow.value = config
    }

    fun getLastStoredConfig(): Config {
        return lastStoredConfig ?: Config()
    }

    override fun read(keys: List<ConfigKey<*>>): Flow<Config> = configFlow

    override suspend fun update(keys: List<ConfigKey<*>>, transform: (Config) -> Config) {
        val newConfig = transform(configFlow.value)
        configFlow.value = newConfig
        lastStoredConfig = newConfig
    }

    override suspend fun clear() {
        configFlow.value = Config()
        wasCleared = true
    }
}
