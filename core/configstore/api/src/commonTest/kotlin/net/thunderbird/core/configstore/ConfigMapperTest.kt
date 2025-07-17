package net.thunderbird.core.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ConfigMapperTest {

    @Test
    fun `toConfig should map object to Config`() {
        // Arrange
        val testSubject = TestConfigMapper()
        val testObject = TestConfig("test value", 123)

        // Act
        val result = testSubject.toConfig(testObject)

        // Assert
        assertThat(result[testSubject.stringKey]).isEqualTo(testObject.stringValue)
        assertThat(result[testSubject.intKey]).isEqualTo(testObject.intValue)
    }

    @Test
    fun `fromConfig should map Config to object`() {
        // Arrange
        val testSubject = TestConfigMapper()
        val config = Config().apply {
            this[testSubject.stringKey] = "test value"
            this[testSubject.intKey] = 123
        }

        // Act
        val result = testSubject.fromConfig(config)

        // Assert
        assertThat(result?.stringValue).isEqualTo("test value")
        assertThat(result?.intValue).isEqualTo(123)
    }

    @Test
    fun `fromConfig should return null when required values are missing`() {
        // Arrange
        val testSubject = TestConfigMapper()
        val config = Config().apply {
            // Missing stringKey value
            this[testSubject.intKey] = 123
        }

        // Act
        val result = testSubject.fromConfig(config)

        // Assert
        assertThat(result).isNull()
    }
}
