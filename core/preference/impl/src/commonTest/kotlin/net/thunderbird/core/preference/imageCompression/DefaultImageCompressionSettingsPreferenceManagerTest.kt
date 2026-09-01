package net.thunderbird.core.preference.imageCompression

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.preference.DefaultPreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.InMemoryStorage
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider

class DefaultImageCompressionSettingsPreferenceManagerTest {
    @Test
    fun `manager should load latest storage on construction and preference changes`() {
        // Arrange
        val logger = TestLogger()
        val storageProvider = object : StorageProvider {
            override var storage: Storage = InMemoryStorage(emptyMap(), logger)

            override fun loadLatestStorage(): Storage = storage

            override fun createStorageEditor(): StorageEditor = error("No writes expected")
        }
        val preferenceChangeBroker = DefaultPreferenceChangeBroker()
        val testSubject = DefaultImageCompressionSettingsPreferenceManager(
            logger = logger,
            storageProvider = storageProvider,
            preferenceChangeBroker = preferenceChangeBroker,
        )
        assertThat(testSubject.getConfig()).isEqualTo(ImageCompressionSettings())

        // Act
        storageProvider.storage = InMemoryStorage(
            mapOf(
                ImageCompressionSettingKey.JpegQuality.value to "70",
                ImageCompressionSettingKey.LongestEdge.value to "1280",
            ),
            logger,
        )
        preferenceChangeBroker.publish(PreferenceScope.IMAGE_COMPRESSION)

        // Assert
        assertThat(testSubject.getConfig()).isEqualTo(ImageCompressionSettings(jpegQuality = 70, longestEdge = 1280))
    }
}
