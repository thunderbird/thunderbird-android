package net.thunderbird.core.featureflag

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import kotlin.test.Test

/**
 * Verifies that the feature flag catalog is bundled as a JVM classpath resource, which is what makes it loadable from
 * plain JVM unit tests, without an Android runtime.
 */
class FeatureFlagCatalogResourceTest {

    @Test
    fun `bundled feature flag catalog should be readable from the classpath`() {
        // Arrange
        val classLoader = FeatureFlagCatalogResourceTest::class.java.classLoader

        // Act
        val catalog = classLoader
            .getResourceAsStream(CATALOG_RESOURCE_NAME)
            ?.bufferedReader()
            ?.use { it.readText() }

        // Assert
        assertThat(catalog).isNotNull().all {
            contains("\"version\"")
            contains("\"flags\"")
            contains("\"overrides\"")
        }
    }

    private companion object {
        const val CATALOG_RESOURCE_NAME = "thunderbird_mobile_featureflag_catalog.json"
    }
}
