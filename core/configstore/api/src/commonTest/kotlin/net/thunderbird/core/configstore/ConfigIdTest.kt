package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConfigIdTest {

    @Test
    fun `should create ConfigId with valid backend and feature`() {
        // Arrange
        val backend = "valid_backend"
        val feature = "valid_feature"

        // Act
        val configId = ConfigId(backend, feature)

        // Assert
        assertThat(configId.backend).isEqualTo(backend)
        assertThat(configId.feature).isEqualTo(feature)
    }

    @Test
    fun `should throw IllegalArgumentException when backend is blank`() {
        // Arrange
        val backend = ""
        val feature = "valid_feature"

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            ConfigId(backend, feature)
        }
    }

    @Test
    fun `should throw IllegalArgumentException when feature is blank`() {
        // Arrange
        val backend = "valid_backend"
        val feature = ""

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            ConfigId(backend, feature)
        }
    }

    @Test
    fun `should throw IllegalArgumentException when backend contains invalid characters`() {
        // Arrange
        val backend = "invalid-backend"
        val feature = "valid_feature"

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            ConfigId(backend, feature)
        }
    }

    @Test
    fun `should throw IllegalArgumentException when feature contains invalid characters`() {
        // Arrange
        val backend = "valid_backend"
        val feature = "invalid-feature"

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            ConfigId(backend, feature)
        }
    }

    @Test
    fun `should allow alphanumeric characters and underscores in backend and feature`() {
        // Arrange
        val backend = "valid_backend_123"
        val feature = "valid_feature_456"

        // Act
        val configId = ConfigId(backend, feature)

        // Assert
        assertThat(configId.backend).isEqualTo(backend)
        assertThat(configId.feature).isEqualTo(feature)
    }
}
