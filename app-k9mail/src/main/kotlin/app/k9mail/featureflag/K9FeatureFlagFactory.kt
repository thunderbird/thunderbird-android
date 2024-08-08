package app.k9mail.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.featureflag.FeatureFlagKey

class K9FeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf(
            FeatureFlag(FeatureFlagKey("material3_navigation_drawer"), false),
        )
    }
}
