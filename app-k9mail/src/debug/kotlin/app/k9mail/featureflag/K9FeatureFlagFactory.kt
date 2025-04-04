package app.k9mail.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.featureflag.toFeatureFlagKey

class K9FeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf(
            FeatureFlag("archive_marks_as_read".toFeatureFlagKey(), enabled = true),
            FeatureFlag("new_account_settings".toFeatureFlagKey(), enabled = true),
            FeatureFlag("disable_font_size_config".toFeatureFlagKey(), enabled = true),
            FeatureFlag("email_notification_default".toFeatureFlagKey(), enabled = true),
            FeatureFlag("enable_dropdown_drawer".toFeatureFlagKey(), enabled = false),
        )
    }
}
