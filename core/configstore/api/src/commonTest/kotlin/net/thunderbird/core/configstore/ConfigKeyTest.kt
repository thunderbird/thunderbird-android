package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class ConfigKeyTest {

    @Test
    fun `BooleanKey should have correct name and type`() {
        // Arrange
        val name = "booleanKey"

        // Act
        val key = ConfigKey.BooleanKey(name)

        // Assert
        assertThat(key.name).isEqualTo(name)
        assertThat(key).isInstanceOf(ConfigKey.BooleanKey::class)
    }

    @Test
    fun `IntKey should have correct name and type`() {
        // Arrange
        val name = "intKey"

        // Act
        val key = ConfigKey.IntKey(name)

        // Assert
        assertThat(key.name).isEqualTo(name)
        assertThat(key).isInstanceOf(ConfigKey.IntKey::class)
    }

    @Test
    fun `StringKey should have correct name and type`() {
        // Arrange
        val name = "stringKey"

        // Act
        val key = ConfigKey.StringKey(name)

        // Assert
        assertThat(key.name).isEqualTo(name)
        assertThat(key).isInstanceOf(ConfigKey.StringKey::class)
    }

    @Test
    fun `LongKey should have correct name and type`() {
        // Arrange
        val name = "longKey"

        // Act
        val key = ConfigKey.LongKey(name)

        // Assert
        assertThat(key.name).isEqualTo(name)
        assertThat(key).isInstanceOf(ConfigKey.LongKey::class)
    }

    @Test
    fun `FloatKey should have correct name and type`() {
        // Arrange
        val name = "floatKey"

        // Act
        val key = ConfigKey.FloatKey(name)

        // Assert
        assertThat(key.name).isEqualTo(name)
        assertThat(key).isInstanceOf(ConfigKey.FloatKey::class)
    }

    @Test
    fun `DoubleKey should have correct name and type`() {
        // Arrange
        val name = "doubleKey"

        // Act
        val key = ConfigKey.DoubleKey(name)

        // Assert
        assertThat(key.name).isEqualTo(name)
        assertThat(key).isInstanceOf(ConfigKey.DoubleKey::class)
    }
}
