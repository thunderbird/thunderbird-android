package net.thunderbird.core.preference.interaction

import assertk.assertThat
import assertk.assertions.isEmpty
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
        assertThat(config.keypadMessageActions).isEmpty()
    }

    @Test
    fun `keypad navigation should load stored number actions without default entries`() = runTest {
        // Arrange
        storeKeypadNavigation(enabled = true)
        storageProvider.createStorageEditor()
            .putString("keypadDigit0Action", KeypadMessageAction.ARCHIVE.name)
            .putString("keypadDigit4Action", KeypadMessageAction.NONE.name)
            .putString("keypadDigit9Action", KeypadMessageAction.TOGGLE_FLAGGED.name)
            .commit()
        val testSubject = createManager()

        // Act
        val config = testSubject.getConfig()

        // Assert
        assertThat(config.isKeypadNavigationEnabled).isTrue()
        assertThat(config.keypadMessageActions).isEqualTo(
            mapOf(0 to KeypadMessageAction.ARCHIVE, 9 to KeypadMessageAction.TOGGLE_FLAGGED),
        )
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
        storageProvider.createStorageEditor()
            .putString("keypadDigit3Action", KeypadMessageAction.DELETE.name)
            .commit()

        // Act
        testSubject.receive(PreferenceScope.INTERACTION)

        // Assert
        assertThat(testSubject.getConfig().isKeypadNavigationEnabled).isTrue()
        assertThat(testSubject.getConfig().keypadMessageActions).isEqualTo(mapOf(3 to KeypadMessageAction.DELETE))
    }

    @Test
    fun `all ten number actions should persist independently with keypad mode disabled`() = runTest {
        // Arrange
        val testSubject = createManager()
        val actions = (0..9).associateWith { digit ->
            when (digit % 3) {
                0 -> KeypadMessageAction.DELETE
                1 -> KeypadMessageAction.ARCHIVE
                else -> KeypadMessageAction.TOGGLE_FLAGGED
            }
        }
        val config = testSubject.getConfig().copy(keypadMessageActions = actions)

        // Act
        testSubject.save(config)
        advanceUntilIdle()
        val reloadedConfig = createManager().getConfig()

        // Assert
        assertThat(reloadedConfig.isKeypadNavigationEnabled).isFalse()
        assertThat(reloadedConfig.keypadMessageActions).isEqualTo(actions)
        for ((digit, action) in actions) {
            assertThat(storageProvider.storage.getString("keypadDigit${digit}Action")).isEqualTo(action.name)
        }
    }

    @Test
    fun `clearing a number action should store NONE without losing other actions`() = runTest {
        // Arrange
        storageProvider.createStorageEditor()
            .putString("keypadDigit0Action", KeypadMessageAction.DELETE.name)
            .putString("keypadDigit5Action", KeypadMessageAction.TOGGLE_FLAGGED.name)
            .commit()
        val testSubject = createManager()

        // Act
        testSubject.save(
            testSubject.getConfig().copy(keypadMessageActions = mapOf(5 to KeypadMessageAction.TOGGLE_FLAGGED)),
        )
        advanceUntilIdle()
        val reloadedConfig = createManager().getConfig()

        // Assert
        assertThat(reloadedConfig.keypadMessageActions).isEqualTo(mapOf(5 to KeypadMessageAction.TOGGLE_FLAGGED))
        for (digit in (0..9) - 5) {
            assertThat(storageProvider.storage.getString("keypadDigit${digit}Action")).isEqualTo("NONE")
        }
    }

    @Test
    fun `obsolete phone and directional actions should not activate number actions`() = runTest {
        // Arrange
        storeKeypadNavigation(enabled = true)
        storageProvider.createStorageEditor()
            .putString("keypadCallAction", KeypadMessageAction.DELETE.name)
            .putString("keypadEndCallAction", KeypadMessageAction.ARCHIVE.name)
            .putString("keypadLeftAction", KeypadMessageAction.TOGGLE_FLAGGED.name)
            .putString("keypadRightAction", KeypadMessageAction.DELETE.name)
            .commit()
        val testSubject = createManager()

        // Act
        val config = testSubject.getConfig()

        // Assert
        assertThat(config.isKeypadNavigationEnabled).isTrue()
        assertThat(config.keypadMessageActions).isEmpty()
    }

    @Test
    fun `saving number actions should filter invalid entries and copy the mutable input before writing`() = runTest {
        // Arrange
        val testSubject = createManager()
        val actions = mutableMapOf(
            0 to KeypadMessageAction.ARCHIVE,
            9 to KeypadMessageAction.DELETE,
            1 to KeypadMessageAction.NONE,
            -1 to KeypadMessageAction.DELETE,
            10 to KeypadMessageAction.DELETE,
        )
        val expected = mapOf(0 to KeypadMessageAction.ARCHIVE, 9 to KeypadMessageAction.DELETE)

        // Act
        testSubject.save(testSubject.getConfig().copy(keypadMessageActions = actions))
        val publishedActions = testSubject.getConfig().keypadMessageActions
        actions.clear()
        actions[3] = KeypadMessageAction.TOGGLE_FLAGGED
        advanceUntilIdle()

        // Assert
        assertThat(publishedActions).isEqualTo(expected)
        assertThat(testSubject.getConfig().keypadMessageActions).isEqualTo(expected)
        assertThat(createManager().getConfig().keypadMessageActions).isEqualTo(expected)
        assertThat(storageProvider.storage.getString("keypadDigit1Action")).isEqualTo("NONE")
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
