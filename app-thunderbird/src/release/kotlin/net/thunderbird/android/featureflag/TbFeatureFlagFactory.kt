package net.thunderbird.android.featureflag

import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.toFeatureFlagKey

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
            FeatureFlag("enable_dropdown_drawer".toFeatureFlagKey(), enabled = false),
            FeatureFlag("enable_dropdown_drawer_ui".toFeatureFlagKey(), enabled = false),
        )
    }
}
