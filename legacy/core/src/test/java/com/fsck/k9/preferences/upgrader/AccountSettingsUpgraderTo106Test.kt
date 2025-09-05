package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.INCOMING_SERVER_SETTINGS_KEY
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

class AccountSettingsUpgraderTo106Test {
    @Test
    fun `should set default folderPathDelimiter when incoming server settings are null`() {
        // Arrange
        val settings = createSettingsMap(incomingServerSettings = null).toMutableMap()
        val testSubject = createTestSubject()
        val expected = createSettingsMap(
            incomingServerSettings = null,
            folderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
        )
        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    @Test
    fun `should set default folderPathDelimiter when incoming server settings path prefix is null`() {
        // Arrange
        val settings = createSettingsMap(incomingServerSettingsPathPrefix = null).toMutableMap()
        val testSubject = createTestSubject()
        val expected = createSettingsMap(
            incomingServerSettingsPathPrefix = null,
            folderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    @Test
    fun `should set default folderPathDelimiter when incoming server settings path prefix is empty`() {
        // Arrange
        val settings = createSettingsMap(incomingServerSettingsPathPrefix = "\"\"").toMutableMap()
        val testSubject = createTestSubject()
        val expected = createSettingsMap(
            incomingServerSettingsPathPrefix = "\"\"",
            folderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    @Test
    fun `should set folderPathDelimiter when incoming server settings path prefix is set`() {
        // Arrange
        val settings = createSettingsMap(incomingServerSettingsPathPrefix = "\".\"").toMutableMap()
        val testSubject = createTestSubject()
        val expected = createSettingsMap(
            incomingServerSettingsPathPrefix = "\".\"",
            folderPathDelimiter = ".",
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    private fun createTestSubject(): AccountSettingsUpgraderTo106 = AccountSettingsUpgraderTo106(
        serverSettingsDtoSerializer = ServerSettingsDtoSerializer(),
    )

    private fun createSettingsMap(
        incomingServerSettingsPathPrefix: String? = null,
        incomingServerSettings: String? =
            """{
            |  "type":"imap","host":"host","port":993,"connectionSecurity":"SSL_TLS_REQUIRED",
            |  "authenticationType":"PLAIN","username":"user","password":"pwd","clientCertificateAlias":null,
            |  "autoDetectNamespace":"true","pathPrefix":$incomingServerSettingsPathPrefix
            |}
            """.trimMargin(),
        folderPathDelimiter: FolderPathDelimiter? = null,
    ): Map<String, Any?> = buildMap {
        incomingServerSettings?.let {
            put(INCOMING_SERVER_SETTINGS_KEY, incomingServerSettings)
        }
        folderPathDelimiter?.let { put(LegacyAccountStorageHandler.FOLDER_PATH_DELIMITER_KEY, it) }
    }
}
