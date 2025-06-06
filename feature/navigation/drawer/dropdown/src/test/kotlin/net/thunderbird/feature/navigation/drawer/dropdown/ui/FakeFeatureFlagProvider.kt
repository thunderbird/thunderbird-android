package net.thunderbird.feature.navigation.drawer.dropdown.ui

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult

class FakeFeatureFlagProvider(
    private val isEnabled: Boolean = false,
) : FeatureFlagProvider {
    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        return if (isEnabled) {
            FeatureFlagResult.Enabled
        } else {
            FeatureFlagResult.Disabled
        }
    }
}
