package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.preferences.ValidatedSettings
import kotlin.test.Test

class CombinedSettingsUpgraderTo104Test {

    private val testSubject = CombinedSettingsUpgraderTo104()

    @Test
    fun `should set avatar monogram from name when available`() {
        // Arrange
        val name = "John Doe"
        val account = createAccount(
            name = name,
        )

        // Act
        val result = testSubject.upgrade(account)

        // Assert
        assertThat(result.settings[AVATAR_MONOGRAM_KEY]).isEqualTo("JO")
    }

    @Test
    fun `should set avatar monogram from name with removed spaces when available`() {
        // Arrange
        val name = "J Doe"
        val account = createAccount(
            name = name,
        )

        // Act
        val result = testSubject.upgrade(account)

        // Assert
        assertThat(result.settings[AVATAR_MONOGRAM_KEY]).isEqualTo("JD")
    }

    @Test
    fun `should set avatar monogram from email when name is not available`() {
        // Arrange
        val email = "test@example.com"
        val account = createAccount(
            email = email,
        )

        // Act
        val result = testSubject.upgrade(account)

        // Assert
        assertThat(result.settings[AVATAR_MONOGRAM_KEY]).isEqualTo("TE")
    }

    @Test
    fun `should set avatar monogram to default when no name or email found`() {
        // Arrange
        val account = createAccount(email = null)

        // Act
        val result = testSubject.upgrade(account)

        // Assert
        assertThat(result.settings[AVATAR_MONOGRAM_KEY]).isEqualTo(AVATAR_MONOGRAM_DEFAULT)
    }

    @Test
    fun `should set avatar monogram to default when name and email are empty`() {
        // Arrange
        val account = createAccount(
            name = "",
            email = "",
        )

        // Act
        val result = testSubject.upgrade(account)

        // Assert
        assertThat(result.settings[AVATAR_MONOGRAM_KEY]).isEqualTo(AVATAR_MONOGRAM_DEFAULT)
    }

    @Test
    fun `should not override existing custom monogram`() {
        // Arrange
        val account = createAccount(
            name = "John Doe",
            settings = mapOf(AVATAR_MONOGRAM_KEY to "JD"),
        )

        // Act
        val result = testSubject.upgrade(account)

        // Assert
        assertThat(result.settings[AVATAR_MONOGRAM_KEY]).isEqualTo("JD")
    }

    @Test
    fun `should override default monogram`() {
        // Arrange
        val account = createAccount(
            name = "John Doe",
            settings = mapOf(AVATAR_MONOGRAM_KEY to AVATAR_MONOGRAM_DEFAULT),
        )

        // Act
        val result = testSubject.upgrade(account)

        // Assert
        assertThat(result.settings[AVATAR_MONOGRAM_KEY]).isEqualTo("JO")
    }

    private fun createAccount(
        name: String? = null,
        email: String? = "test@example.com",
        settings: Map<String, Any?> = emptyMap(),
    ): ValidatedSettings.Account {
        return ValidatedSettings.Account(
            uuid = "uuid",
            name = name,
            incoming = createServer(),
            outgoing = createServer(),
            settings = settings,
            identities = listOfNotNull(
                email?.let {
                    ValidatedSettings.Identity(
                        name = "Identity Name",
                        email = it,
                        description = "Primary",
                        settings = emptyMap(),
                    )
                },
            ),
            folders = emptyList(),
        )
    }

    private fun createServer(): ValidatedSettings.Server {
        return ValidatedSettings.Server(
            type = "irrelevant",
            settings = emptyMap(),
            extras = emptyMap(),
        )
    }

    private companion object {
        const val AVATAR_MONOGRAM_KEY = "avatarMonogram"
        const val AVATAR_MONOGRAM_DEFAULT = "XX"
    }
}
