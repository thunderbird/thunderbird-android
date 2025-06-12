package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AccountSettingsUpgraderTo104Test {

    private val testSubject = AccountSettingsUpgraderTo104()

    @Test
    fun `should set avatar monogram from name when available`() {
        // Arrange
        val name = "John Doe"
        val settings = createSettings(
            name = name,
        )
        val expected = createExpectedSettings(
            name = name,
            monogram = "JO",
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    @Test
    fun `should set avatar monogram from name with removed spaces when available`() {
        // Arrange
        val name = "J Doe"
        val settings = createSettings(
            name = name,
        )
        val expected = createExpectedSettings(
            name = name,
            monogram = "JD",
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    @Test
    fun `should set avatar monogram from email when name is not available`() {
        // Arrange
        val email = "test@example.com"
        val settings = createSettings(
            email = email,
        )
        val expected = createExpectedSettings(
            email = email,
            monogram = "TE",
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    @Test
    fun `should set avatar monogram to default when no name or email found`() {
        // Arrange
        val settings = createSettings()
        val expected = createExpectedSettings(
            monogram = AVATAR_MONOGRAM_DEFAULT,
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    @Test
    fun `should set avatar monogram to default when name and email are empty`() {
        // Arrange
        val settings = createSettings(
            name = "",
            email = "",
        )
        val expected = createExpectedSettings(
            name = "",
            email = "",
            monogram = AVATAR_MONOGRAM_DEFAULT,
        )

        // Act
        testSubject.upgrade(settings)

        // Assert
        assertThat(settings).isEqualTo(expected)
    }

    private fun createSettings(
        name: String? = null,
        email: String? = null,
    ): MutableMap<String, Any?> {
        return mutableMapOf<String, Any?>().apply {
            if (name != null) this[NAME_KEY] = name
            if (email != null) this[EMAIL_KEY] = email
        }
    }

    private fun createExpectedSettings(
        name: String? = null,
        email: String? = null,
        monogram: String,
    ): Map<String, Any?> {
        return mutableMapOf<String, Any?>().apply {
            if (name != null) this[NAME_KEY] = name
            if (email != null) this[EMAIL_KEY] = email
            this[AVATAR_MONOGRAM_KEY] = monogram
        }
    }

    private companion object {

        const val NAME_KEY = "name"
        const val EMAIL_KEY = "email"
        const val AVATAR_MONOGRAM_KEY = "avatarMonogram"

        const val AVATAR_MONOGRAM_DEFAULT = "XX"
    }
}
