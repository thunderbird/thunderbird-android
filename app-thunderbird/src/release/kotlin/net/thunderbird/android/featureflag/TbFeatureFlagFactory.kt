package net.thunderbird.android.featureflag

import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.featureflag.toFeatureFlagKey

/**
 * Feature flags for Thunderbird (release)
 */
class TbFeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf(
            FeatureFlag("archive_marks_as_read".toFeatureFlagKey(), enabled = false),
            FeatureFlag("new_account_settings".toFeatureFlagKey(), enabled = false),
            FeatureFlag("disable_font_size_config".toFeatureFlagKey(), enabled = false),
            FeatureFlag("email_notification_default".toFeatureFlagKey(), enabled = false),
        )
    }
}
