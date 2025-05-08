package net.thunderbird.core.preferences

import kotlinx.coroutines.flow.Flow

interface ConfigManager<T> {
    fun save(config: T)
    fun getConfig(): T
    fun getConfigFlow(): Flow<T>
}
