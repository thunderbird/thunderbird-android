package net.thunderbird.android.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.featureflag.FeatureFlagKey

class TbFeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf(
            FeatureFlag(FeatureFlagKey("material3_navigation_drawer"), true),
            FeatureFlag(FeatureFlagKey("funding_google_play"), false),
        )
    }
}
