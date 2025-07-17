package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class BaseConfigStoreMigrationTest {

    @Test
    fun `should apply migration when version is updated`() = runTest {
        // Arrange
        val initialConfig = TestConfig("initial", 100)
        val migratedConfig = TestConfig("migrated", 200)
        val backend = FakeConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val mapper = TestConfigMapper()

        // Create a test migration that updates the config
        val migration = object : ConfigMigration {
            override suspend fun migrate(currentVersion: Int, newVersion: Int, current: Config): ConfigMigrationResult {
                val updated = Config().apply {
                    this[mapper.stringKey] = migratedConfig.stringValue
                    this[mapper.intKey] = migratedConfig.intValue
                }
                return ConfigMigrationResult.Migrated(updated)
            }
        }

        // Create a config definition with version 2 and our test migration
        val definition = TestConfigDefinition(mapper, initialConfig, version = 2, migration = migration)
        val testSubject = TestConfigStore(provider, definition)

        // Set up backend to return initial config with version 1
        val initialConfigData = Config().apply {
            this[mapper.stringKey] = initialConfig.stringValue
            this[mapper.intKey] = initialConfig.intValue
        }
        backend.setConfig(initialConfigData)

        // Act
        val result = testSubject.config.first()

        // Assert
        assertThat(result.stringValue).isEqualTo(migratedConfig.stringValue)
        assertThat(result.intValue).isEqualTo(migratedConfig.intValue)

        // Verify version was updated
        val versionKey = "_version_test_backend_test_feature"
        assertThat(backend.readVersion(versionKey)).isEqualTo(2)
    }

    @Test
    fun `should remove keys when migration specifies keys to remove`() = runTest {
        // Arrange
        val initialConfig = TestConfig("initial", 100)
        val migratedConfig = TestConfig("migrated", 200)
        val backend = FakeConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val mapper = TestConfigMapper()

        // Create a test migration that updates the config and removes keys
        val migration = object : ConfigMigration {
            override suspend fun migrate(currentVersion: Int, newVersion: Int, current: Config): ConfigMigrationResult {
                val updated = Config().apply {
                    this[mapper.stringKey] = migratedConfig.stringValue
                    this[mapper.intKey] = migratedConfig.intValue
                }
                return ConfigMigrationResult.Migrated(updated, setOf(mapper.intKey))
            }
        }

        // Create a config definition with version 2 and our test migration
        val definition = TestConfigDefinition(mapper, initialConfig, version = 2, migration = migration)
        val testSubject = TestConfigStore(provider, definition)

        // Set up backend to return initial config with version 1
        val initialConfigData = Config().apply {
            this[mapper.stringKey] = initialConfig.stringValue
            this[mapper.intKey] = initialConfig.intValue
        }
        backend.setConfig(initialConfigData)

        // Act
        testSubject.config.first() // Trigger migration

        // Assert
        assertThat(backend.removedKeys.contains(mapper.intKey)).isTrue()
    }

    @Test
    fun `should not apply migration when version is current`() = runTest {
        // Arrange
        val initialConfig = TestConfig("initial", 100)
        val backend = FakeConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val mapper = TestConfigMapper()

        // Create a test migration that should not be called
        var migrationCalled = false
        val migration = object : ConfigMigration {
            override suspend fun migrate(currentVersion: Int, newVersion: Int, current: Config): ConfigMigrationResult {
                migrationCalled = true
                return ConfigMigrationResult.NoOp
            }
        }

        // Create a config definition with version 1 and our test migration
        val definition = TestConfigDefinition(mapper, initialConfig, version = 1, migration = migration)
        val testSubject = TestConfigStore(provider, definition)

        // Set up backend to return initial config with version 1
        val initialConfigData = Config().apply {
            this[mapper.stringKey] = initialConfig.stringValue
            this[mapper.intKey] = initialConfig.intValue
        }
        backend.setConfig(initialConfigData)
        backend.writeVersion("_version_test_backend_test_feature", 1)

        // Act
        val result = testSubject.config.first()

        // Assert
        assertThat(result.stringValue).isEqualTo(initialConfig.stringValue)
        assertThat(result.intValue).isEqualTo(initialConfig.intValue)
        assertThat(migrationCalled).isEqualTo(false)
    }

    @Test
    fun `should apply NoOp migration when specified`() = runTest {
        // Arrange
        val initialConfig = TestConfig("initial", 100)
        val backend = FakeConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val mapper = TestConfigMapper()

        // Create a test migration that returns NoOp
        val migration = object : ConfigMigration {
            override suspend fun migrate(currentVersion: Int, newVersion: Int, current: Config): ConfigMigrationResult {
                return ConfigMigrationResult.NoOp
            }
        }

        // Create a config definition with version 2 and our test migration
        val definition = TestConfigDefinition(mapper, initialConfig, version = 2, migration = migration)
        val testSubject = TestConfigStore(provider, definition)

        // Set up backend to return initial config with version 1
        val initialConfigData = Config().apply {
            this[mapper.stringKey] = initialConfig.stringValue
            this[mapper.intKey] = initialConfig.intValue
        }
        backend.setConfig(initialConfigData)

        // Act
        val result = testSubject.config.first()

        // Assert
        assertThat(result.stringValue).isEqualTo(initialConfig.stringValue)
        assertThat(result.intValue).isEqualTo(initialConfig.intValue)

        // Verify version was updated even though no migration was performed
        val versionKey = "_version_test_backend_test_feature"
        assertThat(backend.readVersion(versionKey)).isEqualTo(2)
    }
}
