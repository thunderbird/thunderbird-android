package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConfigIdTest {

    @Test
    fun `should create ConfigId with valid value`() {
        // Arrange
        val value = "valid_id"

        // Act
        val configId = ConfigId(value)

        // Assert
        assertThat(configId.value).isEqualTo(value)
    }

    @Test
    fun `should throw IllegalArgumentException when value is blank`() {
        // Arrange
        val value = ""

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            ConfigId(value)
        }
    }

    @Test
    fun `should throw IllegalArgumentException when value contains invalid characters`() {
        // Arrange
        val value = "invalid-id"

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            ConfigId(value)
        }
    }

    @Test
    fun `should allow alphanumeric characters and underscores`() {
        // Arrange
        val value = "valid_id_123"

        // Act
        val configId = ConfigId(value)

        // Assert
        assertThat(configId.value).isEqualTo(value)
    }
}
