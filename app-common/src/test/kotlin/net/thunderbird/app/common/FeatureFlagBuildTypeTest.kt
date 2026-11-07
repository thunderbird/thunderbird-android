package net.thunderbird.app.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class FeatureFlagBuildTypeTest {

    @Test
    fun `app build types are returned unchanged`() {
        // Arrange
        val buildTypes = listOf("debug", "daily", "beta", "release")

        // Act
        val result = buildTypes.map { featureFlagBuildType(it) }

        // Assert
        assertThat(result).isEqualTo(buildTypes)
    }

    @Test
    fun `nonMinified build types use the build type they copy`() {
        // Arrange
        val buildTypes = listOf("nonMinifiedDaily", "nonMinifiedBeta", "nonMinifiedRelease")

        // Act
        val result = buildTypes.map { featureFlagBuildType(it) }

        // Assert
        assertThat(result).isEqualTo(listOf("daily", "beta", "release"))
    }

    @Test
    fun `benchmark build types use the build type they copy`() {
        // Arrange
        val buildTypes = listOf("benchmarkDaily", "benchmarkBeta", "benchmarkRelease")

        // Act
        val result = buildTypes.map { featureFlagBuildType(it) }

        // Assert
        assertThat(result).isEqualTo(listOf("daily", "beta", "release"))
    }

    @Test
    fun `build type equal to a prefix is returned unchanged`() {
        // Act
        val result = featureFlagBuildType("benchmark")

        // Assert
        assertThat(result).isEqualTo("benchmark")
    }
}
