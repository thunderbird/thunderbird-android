package net.thunderbird.android.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory

/**
 * Feature flags for Thunderbird Debug
 */
class TbFeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf()
    }
}
