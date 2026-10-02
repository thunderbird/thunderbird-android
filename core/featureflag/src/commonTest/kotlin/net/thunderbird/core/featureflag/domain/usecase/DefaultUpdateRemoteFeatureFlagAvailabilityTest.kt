package net.thunderbird.core.featureflag.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.RemoteCatalogConfig
import net.thunderbird.core.featureflag.model.RemoteCatalogFetchFrequency

class DefaultUpdateRemoteFeatureFlagAvailabilityTest {

    @Test
    fun `invoke should disable the remote catalog when enabled is false`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = configWithRemoteCatalog(enabled = true))
        val testSubject = DefaultUpdateRemoteFeatureFlagAvailability(configStore)

        // Act
        val result = testSubject(enabled = false)

        // Assert
        assertThat(result).isEqualTo(Outcome.success())
        assertThat(configStore.config.first().remoteCatalogConfig.enabled).isEqualTo(false)
    }

    @Test
    fun `invoke should enable the remote catalog when enabled is true`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = configWithRemoteCatalog(enabled = false))
        val testSubject = DefaultUpdateRemoteFeatureFlagAvailability(configStore)

        // Act
        val result = testSubject(enabled = true)

        // Assert
        assertThat(result).isEqualTo(Outcome.success())
        assertThat(configStore.config.first().remoteCatalogConfig.enabled).isEqualTo(true)
    }

    @Test
    fun `invoke should preserve the remaining configuration`() = runTest {
        // Arrange
        val initialConfig = FeatureFlagConfigData(
            remoteCatalogConfig = RemoteCatalogConfig(
                enabled = true,
                fetchFrequency = RemoteCatalogFetchFrequency.Default,
            ),
            targetingKey = TARGETING_KEY,
            overrides = mapOf("flag_a" to true),
        )
        val configStore = FakeFeatureFlagConfigStore(initialConfig = initialConfig)
        val testSubject = DefaultUpdateRemoteFeatureFlagAvailability(configStore)

        // Act
        testSubject(enabled = false)

        // Assert
        assertThat(configStore.config.first()).isEqualTo(
            initialConfig.copy(remoteCatalogConfig = initialConfig.remoteCatalogConfig.copy(enabled = false)),
        )
    }

    @Test
    fun `invoke should fall back to the default configuration when none is persisted`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = null)
        val testSubject = DefaultUpdateRemoteFeatureFlagAvailability(configStore)

        // Act
        val result = testSubject(enabled = false)

        // Assert
        assertThat(result).isEqualTo(Outcome.success())
        assertThat(configStore.config.first()).isEqualTo(
            FeatureFlagConfigData.DEFAULT.copy(
                remoteCatalogConfig = FeatureFlagConfigData.DEFAULT.remoteCatalogConfig.copy(enabled = false),
            ),
        )
    }

    private fun configWithRemoteCatalog(enabled: Boolean) = FeatureFlagConfigData(
        remoteCatalogConfig = RemoteCatalogConfig(enabled = enabled),
    )

    private companion object {
        val TARGETING_KEY: Uuid = Uuid.parse("f2d9a9a0-6d3f-4b5e-9f4a-1d2c3b4a5e6f")
    }
}
