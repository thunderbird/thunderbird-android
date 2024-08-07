package app.k9mail.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory

class K9FeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return emptyList()
    }
}
