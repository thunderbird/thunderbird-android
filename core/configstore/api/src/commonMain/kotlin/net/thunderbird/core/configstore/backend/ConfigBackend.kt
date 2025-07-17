package net.thunderbird.core.configstore.backend

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigKey

/**
 * A config backend that is used to store and retrieve configuration data.
 */
interface ConfigBackend {

    /**
     * Reads the stored configuration from the backend.
     *
     * @param keys The list of configuration keys to read.
     * @return A flow that emits the current configuration.
     */
    fun read(keys: List<ConfigKey<*>>): Flow<Config>

    /**
     * Writes the configuration to the backend.
     *
     * @param keys The list of configuration keys to write.
     * @param transform A function that transforms the current configuration into a new configuration.
     */
    suspend fun update(
        keys: List<ConfigKey<*>>,
        transform: (Config) -> Config,
    )

    /**
     * Clears the stored configuration.
     */
    suspend fun clear()
}
