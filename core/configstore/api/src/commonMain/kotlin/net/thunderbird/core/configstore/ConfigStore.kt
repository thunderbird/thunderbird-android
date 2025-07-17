package net.thunderbird.core.configstore

import kotlinx.coroutines.flow.Flow

/**
 * A generic interface for a configuration store that manages a configuration of type [T].
 *
 * @param T The type of the configuration stored in the config store.
 */
interface ConfigStore<T> {

    /**
     * The configuration stored in the config store, represented as a Flow.
     *
     * @param T The type of the configuration.
     */
    val config: Flow<T?>

    /**
     * Updates the stored configuration by applying the provided transformation function
     *
     * @param transform A function that takes the current configuration and returns a new configuration.
     */
    suspend fun update(transform: (T?) -> T)

    /**
     * Clears the stored configuration.
     */
    suspend fun clear()
}
