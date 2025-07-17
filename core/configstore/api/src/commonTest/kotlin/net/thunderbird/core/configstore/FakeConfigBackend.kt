package net.thunderbird.core.configstore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.thunderbird.core.configstore.backend.ConfigBackend

internal class FakeConfigBackend : ConfigBackend {
    private val configFlow = MutableStateFlow(Config())
    private var lastStoredConfig: Config? = null
    private val versionMap = mutableMapOf<String, Int>()
    var wasCleared = false
    var removedKeys = mutableSetOf<ConfigKey<*>>()

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
        versionMap.clear()
        wasCleared = true
    }

    override suspend fun readVersion(versionKey: String): Int {
        return versionMap[versionKey] ?: 0
    }

    override suspend fun writeVersion(versionKey: String, version: Int) {
        versionMap[versionKey] = version
    }

    override suspend fun removeKeys(keys: Set<ConfigKey<*>>) {
        val currentConfig = configFlow.value
        val entries = currentConfig.toMap().toMutableMap()
        keys.forEach { key -> entries.remove(key) }
        configFlow.value = Config(entries)
        removedKeys.addAll(keys)
    }
}
