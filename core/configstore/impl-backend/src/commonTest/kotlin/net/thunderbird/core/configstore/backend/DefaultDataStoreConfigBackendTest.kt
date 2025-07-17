package net.thunderbird.core.configstore.backend

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.configstore.ConfigKey
import org.junit.Before
import org.junit.Test

class DefaultDataStoreConfigBackendTest {

    private lateinit var testSubject: DefaultDataStoreConfigBackend
    private lateinit var fakeDataStore: FakeDataStore

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        testSubject = DefaultDataStoreConfigBackend(fakeDataStore)
    }

    @Test
    fun `read should return empty config when preferences are empty`() = runTest {
        // Arrange
        val keys = listOf(
            ConfigKey.StringKey("string_key"),
            ConfigKey.IntKey("int_key"),
        )

        // Act
        val result = testSubject.read(keys).first()

        // Assert
        assertThat(result[keys[0]]).isNull()
        assertThat(result[keys[1]]).isNull()
    }

    @Test
    fun `read should return config with values from preferences`() = runTest {
        // Arrange
        val stringKey = ConfigKey.StringKey("string_key")
        val intKey = ConfigKey.IntKey("int_key")
        val booleanKey = ConfigKey.BooleanKey("boolean_key")
        val longKey = ConfigKey.LongKey("long_key")
        val floatKey = ConfigKey.FloatKey("float_key")
        val doubleKey = ConfigKey.DoubleKey("double_key")

        val keys = listOf(stringKey, intKey, booleanKey, longKey, floatKey, doubleKey)

        fakeDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[stringPreferencesKey("string_key")] = "string value"
                this[intPreferencesKey("int_key")] = 123
                this[booleanPreferencesKey("boolean_key")] = true
                this[longPreferencesKey("long_key")] = 456L
                this[floatPreferencesKey("float_key")] = 789.0f
                this[doublePreferencesKey("double_key")] = 101.0
            }
        }

        // Act
        val result = testSubject.read(keys).first()

        // Assert
        assertThat(result[stringKey]).isEqualTo("string value")
        assertThat(result[intKey]).isEqualTo(123)
        assertThat(result[booleanKey]).isEqualTo(true)
        assertThat(result[longKey]).isEqualTo(456L)
        assertThat(result[floatKey]).isEqualTo(789.0f)
        assertThat(result[doubleKey]).isEqualTo(101.0)
    }

    @Test
    fun `update should store values in preferences`() = runTest {
        // Arrange
        val stringKey = ConfigKey.StringKey("string_key")
        val intKey = ConfigKey.IntKey("int_key")
        val keys = listOf(stringKey, intKey)

        // Act
        testSubject.update(keys) { config ->
            config.apply {
                this[stringKey] = "updated string"
                this[intKey] = 456
            }
        }

        // Assert
        val preferences = fakeDataStore.data.first()
        assertThat(preferences[stringPreferencesKey("string_key")]).isEqualTo("updated string")
        assertThat(preferences[intPreferencesKey("int_key")]).isEqualTo(456)
    }

    @Test
    fun `update with empty keys should not change preferences`() = runTest {
        // Arrange
        fakeDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[stringPreferencesKey("string_key")] = "initial value"
            }
        }

        // Act
        testSubject.update(emptyList()) { config ->
            config.apply {
                this[ConfigKey.StringKey("string_key")] = "updated value"
            }
        }

        // Assert
        val preferences = fakeDataStore.data.first()
        assertThat(preferences[stringPreferencesKey("string_key")]).isEqualTo("initial value")
    }

    @Test
    fun `clear should remove all preferences`() = runTest {
        // Arrange
        fakeDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[stringPreferencesKey("string_key")] = "string value"
                this[intPreferencesKey("int_key")] = 123
            }
        }

        // Act
        testSubject.clear()

        // Assert
        val preferences = fakeDataStore.data.first()
        assertThat(preferences.asMap().isEmpty()).isEqualTo(true)
    }

    private class FakeDataStore : DataStore<Preferences> {
        private val emptyPreferences = emptyPreferences()
        private val dataFlow = MutableStateFlow(emptyPreferences)

        override val data: Flow<Preferences> = dataFlow

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val newData = transform(dataFlow.value)
            dataFlow.value = newData
            return newData
        }
    }
}
