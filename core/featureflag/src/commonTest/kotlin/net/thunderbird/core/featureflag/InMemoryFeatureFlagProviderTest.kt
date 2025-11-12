package net.thunderbird.core.featureflag

import assertk.assertThat
import assertk.assertions.isInstanceOf
import org.junit.Test

class InMemoryFeatureFlagProviderTest {

    @Test
    fun `should return FeatureFlagResult#Enabled when feature is enabled`() {
        val feature1Key = FeatureFlagKey("feature1")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = FakeMutableFeatureFlagFactory(
                listOf(
                    FeatureFlag(key = feature1Key, enabled = true),
                ),
            ),
        )

        val result = featureFlagProvider.provide(feature1Key)

        assertThat(result).isInstanceOf<FeatureFlagResult.Enabled>()
    }

    @Test
    fun `should return FeatureFlagResult#Disabled when feature is disabled`() {
        val feature1Key = FeatureFlagKey("feature1")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = FakeMutableFeatureFlagFactory(
                listOf(
                    FeatureFlag(key = feature1Key, enabled = false),
                ),
            ),
        )

        val result = featureFlagProvider.provide(feature1Key)

        assertThat(result).isInstanceOf<FeatureFlagResult.Disabled>()
    }

    @Test
    fun `should return FeatureFlagResult#Unavailable when feature is not found`() {
        val feature1Key = FeatureFlagKey("feature1")
        val feature2Key = FeatureFlagKey("feature2")
        val featureFlagProvider = InMemoryFeatureFlagProvider(
            featureFlagFactory = FakeMutableFeatureFlagFactory(
                listOf(
                    FeatureFlag(key = feature1Key, enabled = false),
                ),
            ),
        )

        val result = featureFlagProvider.provide(feature2Key)

        assertThat(result).isInstanceOf<FeatureFlagResult.Unavailable>()
    }

    private class FakeMutableFeatureFlagFactory(
        override val defaults: List<FeatureFlag>,
        override val overrides: Map<FeatureFlagKey, Boolean> = mapOf(),
    ) : MutableFeatureFlagFactory {
        override fun override(key: FeatureFlagKey, enabled: Boolean) = Unit

        override fun restoreDefaults() = Unit

        override fun createFeatureCatalog(): List<FeatureFlag> = defaults
    }
}
