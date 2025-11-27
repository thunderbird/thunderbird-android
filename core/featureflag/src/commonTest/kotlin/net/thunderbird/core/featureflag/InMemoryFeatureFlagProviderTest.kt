package net.thunderbird.core.featureflag

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryFeatureFlagProviderTest {

    @Test
    fun `should return FeatureFlagResult#Enabled when feature is enabled`() {
        val feature1Key = FeatureFlagKey("feature1")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = { flowOf(listOf(FeatureFlag(key = feature1Key, enabled = true))) },
            featureFlagOverrides = FakeFeatureFlagOverrides(),
            mainDispatcher = UnconfinedTestDispatcher(),
        )

        val result = featureFlagProvider.provide(feature1Key)

        assertThat(result).isInstanceOf<FeatureFlagResult.Enabled>()
    }

    @Test
    fun `should return FeatureFlagResult#Disabled when feature is disabled`() {
        val feature1Key = FeatureFlagKey("feature1")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = { flowOf(listOf(FeatureFlag(key = feature1Key, enabled = false))) },
            featureFlagOverrides = FakeFeatureFlagOverrides(),
            mainDispatcher = UnconfinedTestDispatcher(),
        )

        val result = featureFlagProvider.provide(feature1Key)

        assertThat(result).isInstanceOf<FeatureFlagResult.Disabled>()
    }

    @Test
    fun `should return FeatureFlagResult#Unavailable when feature is not found`() {
        val feature1Key = FeatureFlagKey("feature1")
        val feature2Key = FeatureFlagKey("feature2")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = { flowOf(listOf(FeatureFlag(key = feature1Key, enabled = false))) },
            featureFlagOverrides = FakeFeatureFlagOverrides(),
            mainDispatcher = UnconfinedTestDispatcher(),
        )

        val result = featureFlagProvider.provide(feature2Key)

        assertThat(result).isInstanceOf<FeatureFlagResult.Unavailable>()
    }

    @Test
    fun `should return FeatureFlagResult#Enabled when feature is disabled by default but overridden`() {
        // Arrange
        val key = FeatureFlagKey("feature1")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = { flowOf(listOf(FeatureFlag(key = key, enabled = false))) },
            featureFlagOverrides = FakeFeatureFlagOverrides(
                initialOverrides = mapOf(key to true),
            ),
            mainDispatcher = UnconfinedTestDispatcher(),
        )
        val expected = FeatureFlagResult.Enabled

        // Act
        val actual = featureFlagProvider.provide(key)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should return FeatureFlagResult#Disabled when feature is enabled by default but overridden`() {
        // Arrange
        val key = FeatureFlagKey("feature1")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = { flowOf(listOf(FeatureFlag(key = key, enabled = true))) },
            featureFlagOverrides = FakeFeatureFlagOverrides(
                initialOverrides = mapOf(key to false),
            ),
            mainDispatcher = UnconfinedTestDispatcher(),
        )
        val expected = FeatureFlagResult.Disabled

        // Act
        val actual = featureFlagProvider.provide(key)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private class FakeFeatureFlagOverrides(
        initialOverrides: Map<FeatureFlagKey, Boolean> = emptyMap(),
    ) : FeatureFlagOverrides {
        private val _overrides = MutableStateFlow(initialOverrides)
        override val overrides: StateFlow<Map<FeatureFlagKey, Boolean>> = _overrides.asStateFlow()

        override fun get(key: FeatureFlagKey): Boolean? = overrides.value[key]

        override fun set(key: FeatureFlagKey, value: Boolean) {
            _overrides.update { it + (key to value) }
        }

        override fun clear(key: FeatureFlagKey) {
            _overrides.update { it - key }
        }

        override fun clearAll() {
            _overrides.update { emptyMap() }
        }
    }
}
