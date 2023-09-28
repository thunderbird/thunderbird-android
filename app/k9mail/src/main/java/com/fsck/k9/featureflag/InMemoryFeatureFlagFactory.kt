package com.fsck.k9.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.featureflag.FeatureFlagKey

class InMemoryFeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf(
            FeatureFlag(FeatureFlagKey("new_account_edit"), false),
        )
    }
}
