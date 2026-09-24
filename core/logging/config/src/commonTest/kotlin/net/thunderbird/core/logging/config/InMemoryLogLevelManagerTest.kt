package net.thunderbird.core.logging.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.logging.LogLevel

class InMemoryLogLevelManagerTest {

    @Test
    fun `should initially return default level`() {
        // Arrange
        val testSubject = InMemoryLogLevelManager(defaultLevel = LogLevel.INFO)

        // Act
        val result = testSubject.current()

        // Assert
        assertThat(result).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `should return overridden level`() {
        // Arrange
        val testSubject = InMemoryLogLevelManager(defaultLevel = LogLevel.INFO)

        // Act
        testSubject.override(LogLevel.DEBUG)

        // Assert
        assertThat(testSubject.current()).isEqualTo(LogLevel.DEBUG)
    }

    @Test
    fun `should restore default level`() {
        // Arrange
        val testSubject = InMemoryLogLevelManager(defaultLevel = LogLevel.INFO)
        testSubject.override(LogLevel.DEBUG)

        // Act
        testSubject.restoreDefault()

        // Assert
        assertThat(testSubject.current()).isEqualTo(LogLevel.INFO)
    }
}
