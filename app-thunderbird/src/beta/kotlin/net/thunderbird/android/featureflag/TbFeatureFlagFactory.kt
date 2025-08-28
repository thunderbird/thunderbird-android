package net.thunderbird.android.featureflag

import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.toFeatureFlagKey

/**
 * Feature flags for Thunderbird Beta
 */
class TbFeatureFlagFactory : FeatureFlagFactory {
    override fun createFeatureCatalog(): List<FeatureFlag> {
        return listOf(
            FeatureFlag("archive_marks_as_read".toFeatureFlagKey(), enabled = true),
            FeatureFlag("new_account_settings".toFeatureFlagKey(), enabled = true),
            FeatureFlag("disable_font_size_config".toFeatureFlagKey(), enabled = true),
            FeatureFlag("email_notification_default".toFeatureFlagKey(), enabled = true),
            FeatureFlag("enable_dropdown_drawer".toFeatureFlagKey(), enabled = true),
            FeatureFlag("enable_dropdown_drawer_ui".toFeatureFlagKey(), enabled = true),
            FeatureFlag(FeatureFlagKey.DisplayInAppNotifications, enabled = true),
            FeatureFlag(FeatureFlagKey.UseNotificationSenderForSystemNotifications, enabled = false),
        )
    }
}
