package net.thunderbird.core.featureflag.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isNotEmpty
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer

class LocalFeatureFlagCatalogDataSourceTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `load should return the bundled feature flag catalog`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val registrySerializer = FlagRegistryOverrideSerializer(
            k9Factory = EmptyAppVariantOverride,
            thunderbirdFactory = EmptyAppVariantOverride,
        )
        val testSubject = LocalFeatureFlagCatalogDataSource(
            jsonParser = FeatureFlagCatalogJsonParser(registrySerializer),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        // Act
        testSubject.load().test {
            val catalog = awaitItem()

            // Assert
            assertThat(catalog.flags).isNotEmpty()
        }
    }
}
