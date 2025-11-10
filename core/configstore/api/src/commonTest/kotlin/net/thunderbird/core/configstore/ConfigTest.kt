package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ConfigTest {

    @Test
    fun `should return null when key is not set`() {
        // Arrange
        val config = Config()
        val key = ConfigKey.StringKey("test")

        // Act
        val result = config[key]

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `should return value when key is set`() {
        // Arrange
        val config = Config()
        val key = ConfigKey.StringKey("test")
        val value = "value"
        config[key] = value

        // Act
        val result = config[key]

        // Assert
        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `should update value when key is set`() {
        // Arrange
        val config = Config()
        val key = ConfigKey.StringKey("test")
        val initialValue = "initial"
        val updatedValue = "updated"
        config[key] = initialValue

        // Act
        config[key] = updatedValue
        val result = config[key]

        // Assert
        assertThat(result).isEqualTo(updatedValue)
    }

    @Test
    fun `should handle different key types`() {
        // Arrange
        val config = Config()
        val stringKey = ConfigKey.StringKey("string")
        val intKey = ConfigKey.IntKey("int")
        val booleanKey = ConfigKey.BooleanKey("boolean")
        val longKey = ConfigKey.LongKey("long")
        val floatKey = ConfigKey.FloatKey("float")
        val doubleKey = ConfigKey.DoubleKey("double")

        // Act
        config[stringKey] = "string value"
        config[intKey] = 123
        config[booleanKey] = true
        config[longKey] = 123L
        config[floatKey] = 123.45f
        config[doubleKey] = 123.45

        // Assert
        assertThat(config[stringKey]).isEqualTo("string value")
        assertThat(config[intKey]).isEqualTo(123)
        assertThat(config[booleanKey]).isEqualTo(true)
        assertThat(config[longKey]).isEqualTo(123L)
        assertThat(config[floatKey]).isEqualTo(123.45f)
        assertThat(config[doubleKey]).isEqualTo(123.45)
    }

    @Test
    fun `toMap should return a map of all entries`() {
        // Arrange
        val config = Config()
        val key1 = ConfigKey.StringKey("key1")
        val key2 = ConfigKey.IntKey("key2")
        config[key1] = "value1"
        config[key2] = 2

        // Act
        val map = config.toMap()

        // Assert
        assertThat(map.size).isEqualTo(2)
        assertThat(map[key1]).isEqualTo("value1")
        assertThat(map[key2]).isEqualTo(2)
    }
}
