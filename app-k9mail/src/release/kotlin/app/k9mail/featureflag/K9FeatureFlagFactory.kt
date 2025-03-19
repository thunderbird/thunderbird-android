package app.k9mail.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.featureflag.toFeatureFlagKey

class K9FeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf(
            FeatureFlag("archive_marks_as_read".toFeatureFlagKey(), enabled = false),
            FeatureFlag("new_account_settings".toFeatureFlagKey(), enabled = false),
            FeatureFlag("disable_font_size_config".toFeatureFlagKey(), enabled = false),
        )
    }
}
