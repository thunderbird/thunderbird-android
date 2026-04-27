package net.thunderbird.feature.thundermail.ui

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test

class RegisteredTrademarkInjectorTest {

    private val testSubject = RegisteredTrademarkInjector

    @Test
    fun `inject should return text unchanged when no Thunderbird present`() {
        // Arrange
        val input = "Hello World"

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.text).isEqualTo("Hello World")
    }

    @Test
    fun `inject should append trademark symbol after Thunderbird`() {
        // Arrange
        val input = "Thunderbird"

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.text).isEqualTo("Thunderbird®")
    }

    @Test
    fun `inject should append trademark symbol when Thunderbird is within text`() {
        // Arrange
        val input = "Welcome to Thunderbird Mail"

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.text).isEqualTo("Welcome to Thunderbird® Mail")
    }

    @Test
    fun `inject should handle multiple occurrences of Thunderbird`() {
        // Arrange
        val input = "Thunderbird and Thunderbird"

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.text).isEqualTo("Thunderbird® and Thunderbird®")
    }

    @Test
    fun `inject should return empty string when input is empty`() {
        // Arrange
        val input = ""

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.text).isEqualTo("")
    }

    @Test
    fun `inject should append trademark after partial match like Thunderbirds`() {
        // Arrange
        val input = "Thunderbirds"

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.text).isEqualTo("Thunderbird®s")
    }

    @Test
    fun `inject should have superscript span style on trademark symbol`() {
        // Arrange
        val input = "Thunderbird"

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.spanStyles).hasSize(1)
        val spanStyle = result.spanStyles.first()
        assertThat(spanStyle.start).isEqualTo("Thunderbird".length)
        assertThat(spanStyle.end).isEqualTo("Thunderbird®".length)
    }

    @Test
    fun `inject should have correct span positions for multiple occurrences`() {
        // Arrange
        val input = "Use Thunderbird with Thunderbird"

        // Act
        val result = testSubject.inject(input)

        // Assert
        assertThat(result.spanStyles).hasSize(2)
    }
}
