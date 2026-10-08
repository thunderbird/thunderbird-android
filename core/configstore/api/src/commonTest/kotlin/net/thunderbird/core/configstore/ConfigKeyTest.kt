package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
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

    @Test
    fun `equals should return true for same key type and same name`() {
        val key1 = ConfigKey.StringKey("test")
        val key2 = ConfigKey.StringKey("test")

        assertThat(key1).isEqualTo(key2)
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode())
    }

    @Test
    fun `equals should return false for different key type and same name`() {
        val key1 = ConfigKey.StringKey("test")
        val key2 = ConfigKey.IntKey("test")

        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun `equals should return false for same key type and different name`() {
        val key1 = ConfigKey.StringKey("test1")
        val key2 = ConfigKey.StringKey("test2")

        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun `toString should return correct representation`() {
        val key = ConfigKey.IntKey("myKey")

        assertThat(key.toString()).isEqualTo("IntKey(name='myKey')")
    }
}
