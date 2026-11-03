package net.thunderbird.core.preference.interaction

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.preference.DefaultPreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.InMemoryStorage
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInteractionSettingsPreferenceManagerTest {
    private val storageProvider = FakeStorageProvider()

    @Test
    fun `keypad navigation should be disabled when no preference is stored`() = runTest {
        // Arrange
        val testSubject = createManager()

        // Act
        val config = testSubject.getConfig()

        // Assert
        assertThat(config.isKeypadNavigationEnabled).isFalse()
        assertThat(config.keypadCallAction).isEqualTo(KeypadMessageAction.NONE)
        assertThat(config.keypadEndCallAction).isEqualTo(KeypadMessageAction.NONE)
    }

    @Test
    fun `keypad navigation should load the stored preference`() = runTest {
        // Arrange
        storeKeypadNavigation(enabled = true)
        val testSubject = createManager()

        // Act
        val config = testSubject.getConfig()

        // Assert
        assertThat(config.isKeypadNavigationEnabled).isTrue()
    }

    @Test
    fun `keypad navigation should remain enabled after reloading settings`() = runTest {
        // Arrange
        val testSubject = createManager()

        // Act
        testSubject.save(testSubject.getConfig().copy(isKeypadNavigationEnabled = true))
        advanceUntilIdle()
        val reloadedConfig = createManager().getConfig()

        // Assert
        assertThat(testSubject.getConfig().isKeypadNavigationEnabled).isTrue()
        assertThat(reloadedConfig.isKeypadNavigationEnabled).isTrue()
    }

    @Test
    fun `disabling keypad navigation should persist after reloading settings`() = runTest {
        // Arrange
        storeKeypadNavigation(enabled = true)
        val testSubject = createManager()

        // Act
        testSubject.save(testSubject.getConfig().copy(isKeypadNavigationEnabled = false))
        advanceUntilIdle()
        val reloadedConfig = createManager().getConfig()

        // Assert
        assertThat(reloadedConfig.isKeypadNavigationEnabled).isFalse()
    }

    @Test
    fun `interaction preference changes should reload keypad navigation`() = runTest {
        // Arrange
        val testSubject = createManager()
        storeKeypadNavigation(enabled = true)

        // Act
        testSubject.receive(PreferenceScope.INTERACTION)

        // Assert
        assertThat(testSubject.getConfig().isKeypadNavigationEnabled).isTrue()
    }

    @Test
    fun `call and end-call actions should persist independently with keypad mode disabled`() = runTest {
        // Arrange
        val testSubject = createManager()
        val config = testSubject.getConfig().copy(
            keypadCallAction = KeypadMessageAction.ARCHIVE,
            keypadEndCallAction = KeypadMessageAction.DELETE,
        )

        // Act
        testSubject.save(config)
        advanceUntilIdle()
        val reloadedConfig = createManager().getConfig()

        // Assert
        assertThat(reloadedConfig.isKeypadNavigationEnabled).isFalse()
        assertThat(reloadedConfig.keypadCallAction).isEqualTo(KeypadMessageAction.ARCHIVE)
        assertThat(reloadedConfig.keypadEndCallAction).isEqualTo(KeypadMessageAction.DELETE)
    }

    @Test
    fun `call action should be clearable without losing the end-call action`() = runTest {
        // Arrange
        storageProvider.createStorageEditor()
            .putString(InteractionSettingKey.KeypadCallAction.value, KeypadMessageAction.DELETE.name)
            .putString(InteractionSettingKey.KeypadEndCallAction.value, KeypadMessageAction.TOGGLE_FLAGGED.name)
            .commit()
        val testSubject = createManager()

        // Act
        testSubject.save(testSubject.getConfig().copy(keypadCallAction = KeypadMessageAction.NONE))
        advanceUntilIdle()
        val reloadedConfig = createManager().getConfig()

        // Assert
        assertThat(reloadedConfig.keypadCallAction).isEqualTo(KeypadMessageAction.NONE)
        assertThat(reloadedConfig.keypadEndCallAction).isEqualTo(KeypadMessageAction.TOGGLE_FLAGGED)
    }

    private fun TestScope.createManager() = DefaultInteractionSettingsPreferenceManager(
        logger = TestLogger(),
        storageProvider = storageProvider,
        ioDispatcher = StandardTestDispatcher(testScheduler),
        scope = this,
        preferenceChangeBroker = DefaultPreferenceChangeBroker(),
    )

    private fun storeKeypadNavigation(enabled: Boolean) {
        storageProvider.createStorageEditor()
            .putBoolean(InteractionSettingKey.KeypadNavigationEnabled.value, enabled)
            .commit()
    }

    private class FakeStorageProvider : StorageProvider {
        private var values = emptyMap<String, String>()

        override val storage: Storage
            get() = InMemoryStorage(values, TestLogger())

        override fun createStorageEditor(): StorageEditor = object : StorageEditor {
            private val changes = values.toMutableMap()

            override fun putBoolean(key: String, value: Boolean): StorageEditor = putString(key, value.toString())

            override fun putInt(key: String, value: Int): StorageEditor = putString(key, value.toString())

            override fun putLong(key: String, value: Long): StorageEditor = putString(key, value.toString())

            override fun putString(key: String, value: String?): StorageEditor = apply {
                if (value == null) {
                    changes.remove(key)
                } else {
                    changes[key] = value
                }
            }

            override fun remove(key: String): StorageEditor = putString(key, null)

            override fun commit(): Boolean {
                values = changes.toMap()
                return true
            }
        }
    }
}
