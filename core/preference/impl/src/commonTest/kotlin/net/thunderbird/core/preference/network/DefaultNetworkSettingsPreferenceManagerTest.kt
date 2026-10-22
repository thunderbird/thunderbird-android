package net.thunderbird.core.preference.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.BackgroundOps
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.InMemoryStorage
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.StorageUpdater

class DefaultNetworkSettingsPreferenceManagerTest {

    private val logger = TestLogger()

    @Test
    fun `getConfig should return check mail on app start disabled by default`() = runTest {
        // Arrange
        val testSubject = createTestSubject(storedValues = emptyMap())

        // Act
        val config = testSubject.getConfig()

        // Assert
        assertThat(config.isCheckMailOnAppStart).isFalse()
    }

    @Test
    fun `getConfig should return check mail on app start from storage`() = runTest {
        // Arrange
        val testSubject = createTestSubject(
            storedValues = mapOf(NetworkSettingKey.CheckMailOnAppStart.value to "true"),
        )

        // Act
        val config = testSubject.getConfig()

        // Assert
        assertThat(config.isCheckMailOnAppStart).isTrue()
    }

    @Test
    fun `save should persist check mail on app start and update config`() = runTest {
        // Arrange
        val storageEditor = FakeStorageEditor()
        val testSubject = createTestSubject(storedValues = emptyMap(), storageEditor = storageEditor)
        val config = NetworkSettings(
            backgroundOps = BackgroundOps.NEVER,
            isCheckMailOnAppStart = true,
        )

        // Act
        testSubject.save(config)

        // Assert
        assertThat(storageEditor.values).isEqualTo(
            mapOf(
                NetworkSettingKey.BackgroundOperations.value to BackgroundOps.NEVER.name,
                NetworkSettingKey.CheckMailOnAppStart.value to "true",
            ),
        )
        assertThat(storageEditor.commitCount).isEqualTo(1)
        assertThat(testSubject.getConfig()).isEqualTo(config)
    }

    @Test
    fun `receive should reload check mail on app start when network scope changes`() = runTest {
        // Arrange
        val storagePersister = FakeStoragePersister(emptyMap())
        val testSubject = createTestSubject(storagePersister = storagePersister)
        storagePersister.values = mapOf(NetworkSettingKey.CheckMailOnAppStart.value to "true")

        // Act
        testSubject.receive(PreferenceScope.NETWORK)

        // Assert
        assertThat(testSubject.getConfig().isCheckMailOnAppStart).isTrue()
    }

    @Test
    fun `receive should not reload config when other scope changes`() = runTest {
        // Arrange
        val storagePersister = FakeStoragePersister(emptyMap())
        val testSubject = createTestSubject(storagePersister = storagePersister)
        storagePersister.values = mapOf(NetworkSettingKey.CheckMailOnAppStart.value to "true")

        // Act
        testSubject.receive(PreferenceScope.PRIVACY)

        // Assert
        assertThat(testSubject.getConfig().isCheckMailOnAppStart).isFalse()
    }

    private fun TestScope.createTestSubject(
        storedValues: Map<String, String> = emptyMap(),
        storagePersister: FakeStoragePersister = FakeStoragePersister(storedValues),
        storageEditor: StorageEditor = FakeStorageEditor(),
    ): DefaultNetworkSettingsPreferenceManager = DefaultNetworkSettingsPreferenceManager(
        logger = logger,
        storagePersister = storagePersister,
        storageEditor = storageEditor,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        scope = backgroundScope,
        preferenceChangeBroker = FakePreferenceChangeBroker(),
    )

    private inner class FakeStoragePersister(
        var values: Map<String, String>,
    ) : StoragePersister {
        override fun loadValues(): Storage = InMemoryStorage(values, logger)

        override fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor = FakeStorageEditor()
    }

    private class FakeStorageEditor : StorageEditor {
        val values = mutableMapOf<String, String>()
        var commitCount = 0
            private set

        override fun putBoolean(key: String, value: Boolean): StorageEditor = apply {
            values[key] = value.toString()
        }

        override fun putInt(key: String, value: Int): StorageEditor = apply {
            values[key] = value.toString()
        }

        override fun putLong(key: String, value: Long): StorageEditor = apply {
            values[key] = value.toString()
        }

        override fun putString(key: String, value: String?): StorageEditor = apply {
            if (value == null) values.remove(key) else values[key] = value
        }

        override fun remove(key: String): StorageEditor = apply {
            values.remove(key)
        }

        override fun commit(): Boolean {
            commitCount++
            return true
        }
    }

    private class FakePreferenceChangeBroker : PreferenceChangeBroker {
        override fun subscribe(subscriber: PreferenceChangeSubscriber) = Unit

        override fun unsubscribe(subscriber: PreferenceChangeSubscriber) = Unit
    }
}
