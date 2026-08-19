package net.thunderbird.core.featureflag.data.configstore

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.backend.ConfigBackend
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.configstore.testing.TestConfigBackend

@OptIn(ExperimentalUuidApi::class)
class DefaultFeatureFlagConfigStoreTest {

    @Test
    fun `config should round trip the overrides through the backend`() = runTest {
        // Arrange
        val backend = TestConfigBackend()
        val testSubject = createTestSubject(backend)
        testSubject.update { current: FeatureFlagConfigData ->
            current.copy(overrides = mapOf("flag_a" to true, "flag_b" to false))
        }

        // Act
        val result = createTestSubject(backend).config.first()

        // Assert
        assertThat(result.overrides).containsOnly("flag_a" to true, "flag_b" to false)
    }

    @Test
    fun `config should round trip the targeting key together with the overrides`() = runTest {
        // Arrange
        val backend = TestConfigBackend()
        val testSubject = createTestSubject(backend)
        testSubject.update { current: FeatureFlagConfigData ->
            current.copy(targetingKey = TARGETING_KEY, overrides = mapOf("flag_a" to true))
        }

        // Act
        val result = createTestSubject(backend).config.first()

        // Assert
        assertThat(result.targetingKey).isEqualTo(TARGETING_KEY)
        assertThat(result.overrides).containsOnly("flag_a" to true)
    }

    @Test
    fun `config should drop the persisted overrides when they are removed`() = runTest {
        // Arrange
        val backend = TestConfigBackend()
        val testSubject = createTestSubject(backend)
        testSubject.update { current: FeatureFlagConfigData -> current.copy(overrides = mapOf("flag_a" to true)) }

        // Act
        testSubject.update { current: FeatureFlagConfigData -> current.copy(overrides = emptyMap()) }

        // Assert
        assertThat(createTestSubject(backend).config.first().overrides).isEmpty()
    }

    @Test
    fun `config should return no overrides when the persisted value is not readable`() = runTest {
        // Arrange
        val backend = TestConfigBackend(
            initialConfig = Config().apply {
                this[FeatureFlagConfigKeys.OVERRIDES] = "not-json"
            },
        )

        // Act
        val result = createTestSubject(backend).config.first()

        // Assert
        assertThat(result.overrides).isEmpty()
    }

    private fun createTestSubject(backend: ConfigBackend): FeatureFlagConfigStore = DefaultFeatureFlagConfigStore(
        id = CONFIG_ID,
        provider = SingleBackendConfigBackendProvider(backend),
    )

    private companion object {
        val CONFIG_ID = ConfigId(backend = "test", feature = "featureflag")
        val TARGETING_KEY: Uuid = Uuid.parse("f2d9a9a0-6d3f-4b5e-9f4a-1d2c3b4a5e6f")
    }
}

private class SingleBackendConfigBackendProvider(
    private val backend: ConfigBackend,
) : ConfigBackendProvider {
    override fun provide(id: ConfigId): ConfigBackend = backend
}
