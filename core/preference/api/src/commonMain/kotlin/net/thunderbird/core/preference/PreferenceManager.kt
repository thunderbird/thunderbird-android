package net.thunderbird.core.preference

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing preferences of a specific type.
 *
 * This interface provides methods to save, retrieve, and observe changes to a configuration object.
 *
 * @param T The type of the configuration object being managed.
 */
interface PreferenceManager<T> {
    /**
     * Saves the given configuration.
     *
     * @param config The configuration of type [T] to be saved.
     */
    fun save(config: T)

    /**
     * Retrieves a snapshot of the current configuration.
     *
     * **Note:** This function provides a one-time snapshot of the configuration.
     * For observing configuration changes reactively, it is recommended to use [getConfigFlow] instead.
     *
     * @return The current configuration of type [T].
     */
    fun getConfig(): T

    /**
     * Returns a [Flow] that emits the configuration whenever it changes.
     *
     * This allows observing changes to the configuration in a reactive way.
     *
     * @return A [Flow] of [T] representing the configuration.
     */
    fun getConfigFlow(): Flow<T>
}

/**
 * Updates the configuration by applying the given [updater] function to the current configuration.
 *
 * This function is an inline extension function for [PreferenceManager].
 * It retrieves the current configuration, applies the [updater] function to it,
 * and then saves the updated configuration.
 *
 * @param T The type of the configuration.
 * @param updater A lambda function that takes the current configuration of type [T]
 *  and returns the updated configuration of type [T].
 */
inline fun <reified T> PreferenceManager<T>.update(updater: (T) -> T) {
    val config = getConfig()
    save(updater(config))
}
