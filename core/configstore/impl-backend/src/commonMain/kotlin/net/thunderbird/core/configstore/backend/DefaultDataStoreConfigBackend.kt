package net.thunderbird.core.configstore.backend

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.collections.iterator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigKey

/**
 * A low-level implementation of a configuration store backed by DataStore Preferences.
 * This class is responsible for serialization, deserialization, and persistence.
 */
class DefaultDataStoreConfigBackend(
    private val dataStore: DataStore<Preferences>,
) : DataStoreConfigBackend {

    override fun read(keys: List<ConfigKey<*>>): Flow<Config> = dataStore.data.map { preferences ->
        val config = Config()
        keys.forEach { key ->
            when (key) {
                is ConfigKey.BooleanKey -> preferences[booleanPreferencesKey(key.name)]?.let { config[key] = it }
                is ConfigKey.IntKey -> preferences[intPreferencesKey(key.name)]?.let { config[key] = it }
                is ConfigKey.StringKey -> preferences[stringPreferencesKey(key.name)]?.let { config[key] = it }
                is ConfigKey.LongKey -> preferences[longPreferencesKey(key.name)]?.let { config[key] = it }
                is ConfigKey.FloatKey -> preferences[floatPreferencesKey(key.name)]?.let { config[key] = it }
                is ConfigKey.DoubleKey -> preferences[doublePreferencesKey(key.name)]?.let { config[key] = it }
            }
        }
        config
    }

    override suspend fun update(keys: List<ConfigKey<*>>, transform: (Config) -> Config) {
        val current = read(keys).first()
        val updated = transform(current)

        if (keys.isEmpty()) return

        dataStore.edit { preferences ->
            for ((key, value) in updated.toMap()) {
                when (key) {
                    is ConfigKey.BooleanKey -> if (value is Boolean) {
                        preferences[booleanPreferencesKey(key.name)] = value
                    }

                    is ConfigKey.IntKey -> if (value is Int) preferences[intPreferencesKey(key.name)] = value
                    is ConfigKey.StringKey -> if (value is String) preferences[stringPreferencesKey(key.name)] = value
                    is ConfigKey.LongKey -> if (value is Long) preferences[longPreferencesKey(key.name)] = value
                    is ConfigKey.FloatKey -> if (value is Float) preferences[floatPreferencesKey(key.name)] = value
                    is ConfigKey.DoubleKey -> if (value is Double) preferences[doublePreferencesKey(key.name)] = value
                }
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override suspend fun readVersion(versionKey: String): Int {
        return dataStore.data.map { preferences ->
            preferences[intPreferencesKey(versionKey)] ?: 0
        }.first()
    }

    override suspend fun writeVersion(versionKey: String, version: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey(versionKey)] = version
        }
    }

    override suspend fun removeKeys(keys: Set<ConfigKey<*>>) {
        dataStore.edit { preferences ->
            for (key in keys) {
                when (key) {
                    is ConfigKey.BooleanKey -> preferences.remove(booleanPreferencesKey(key.name))
                    is ConfigKey.IntKey -> preferences.remove(intPreferencesKey(key.name))
                    is ConfigKey.StringKey -> preferences.remove(stringPreferencesKey(key.name))
                    is ConfigKey.LongKey -> preferences.remove(longPreferencesKey(key.name))
                    is ConfigKey.FloatKey -> preferences.remove(floatPreferencesKey(key.name))
                    is ConfigKey.DoubleKey -> preferences.remove(doublePreferencesKey(key.name))
                }
            }
        }
    }
}
