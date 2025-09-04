package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class BaseConfigStoreTest {

    @Test
    fun `config should return mapped value from backend`() = runTest {
        // Arrange
        val testConfig = TestConfig("test value", 123)
        val backend = FakeConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val mapper = TestConfigMapper()
        val definition = TestConfigDefinition(mapper, testConfig)
        val testSubject = TestConfigStore(provider, definition)

        // Set up backend to return a config
        val config = Config().apply {
            this[mapper.stringKey] = testConfig.stringValue
            this[mapper.intKey] = testConfig.intValue
        }
        backend.setConfig(config)

        // Act
        val result = testSubject.config.first()

        // Assert
        assertThat(result?.stringValue).isEqualTo(testConfig.stringValue)
        assertThat(result?.intValue).isEqualTo(testConfig.intValue)
    }

    @Test
    fun `update should transform the config and store it in backend`() = runTest {
        // Arrange
        val initialConfig = TestConfig("initial", 100)
        val updatedConfig = TestConfig("updated", 200)
        val backend = FakeConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val mapper = TestConfigMapper()
        val definition = TestConfigDefinition(mapper, initialConfig)
        val testSubject = TestConfigStore(provider, definition)

        // Set up backend to return initial config
        val initialConfigData = Config().apply {
            this[mapper.stringKey] = initialConfig.stringValue
            this[mapper.intKey] = initialConfig.intValue
        }
        backend.setConfig(initialConfigData)

        // Act
        testSubject.update { _ -> updatedConfig }

        // Assert
        val storedConfig = backend.getLastStoredConfig()
        assertThat(storedConfig[mapper.stringKey]).isEqualTo(updatedConfig.stringValue)
        assertThat(storedConfig[mapper.intKey]).isEqualTo(updatedConfig.intValue)
    }

    @Test
    fun `clear should clear the backend`() = runTest {
        // Arrange
        val testConfig = TestConfig("test value", 123)
        val backend = FakeConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val mapper = TestConfigMapper()
        val definition = TestConfigDefinition(mapper, testConfig)
        val testSubject = TestConfigStore(provider, definition)

        // Act
        testSubject.clear()

        // Assert
        assertThat(backend.wasCleared).isEqualTo(true)
    }
}
