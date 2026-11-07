package com.fsck.k9.preferences

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContainKey
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.legacy.logging.Log
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class FolderSettingsUpgraderTest {
    private val testSubject = FolderSettingsUpgrader(generalSettingsManager = mock())

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `upgrading a folder from version 112 should drop the unified inbox setting`() {
        // Arrange
        val folder = FolderSettingsValidator().validate(
            contentVersion = 112,
            folder = SettingsFile.Folder(
                name = "INBOX",
                settings = mapOf("integrate" to "true", "inTopGroup" to "true"),
            ),
        )

        // Act
        val result = testSubject.upgrade(targetVersion = 113, contentVersion = 112, folder = folder)

        // Assert
        assertThat(result.settings).doesNotContainKey("integrate")
        assertThat(result.settings).contains("inTopGroup" to true)
    }
}
