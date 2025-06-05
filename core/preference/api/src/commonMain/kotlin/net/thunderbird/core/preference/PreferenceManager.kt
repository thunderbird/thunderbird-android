package net.thunderbird.core.preference

import kotlinx.coroutines.flow.Flow

interface PreferenceManager<T> {
    fun save(config: T)
    fun getConfig(): T
    fun getConfigFlow(): Flow<T>
}
