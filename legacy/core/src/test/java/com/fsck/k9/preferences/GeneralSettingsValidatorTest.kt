package com.fsck.k9.preferences

import assertk.assertThat
import assertk.assertions.doesNotContainKey
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.K9RobolectricTest
import org.junit.Test

class GeneralSettingsValidatorTest : K9RobolectricTest() {
    private val testSubject = GeneralSettingsValidator()

    @Test
    fun `validate() should accept checkMailOnAppStart in version 112`() {
        // Arrange
        val settings = mapOf("checkMailOnAppStart" to "true")

        // Act
        val result = testSubject.validate(contentVersion = 112, settings = settings)

        // Assert
        assertThat(result).key("checkMailOnAppStart").isEqualTo(true)
    }

    @Test
    fun `validate() should skip invalid checkMailOnAppStart`() {
        // Arrange
        val settings = mapOf("checkMailOnAppStart" to "invalid")

        // Act
        val result = testSubject.validate(contentVersion = 112, settings = settings)

        // Assert
        assertThat(result).doesNotContainKey("checkMailOnAppStart")
    }

    @Test
    fun `validate() should ignore checkMailOnAppStart before version 112`() {
        // Arrange
        val settings = mapOf("checkMailOnAppStart" to "true")

        // Act
        val result = testSubject.validate(contentVersion = 111, settings = settings)

        // Assert
        assertThat(result).doesNotContainKey("checkMailOnAppStart")
    }
}
