package app.k9mail.featureflag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey

class K9FeatureFlagFactory : FeatureFlagFactory {
    override fun getCatalog(): Flow<List<FeatureFlag>> = flow {
        emit(
            listOf(
                FeatureFlag(GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ, enabled = true),
                FeatureFlag(GeneratedFeatureFlagKey.DISABLE_FONT_SIZE_CONFIG, enabled = true),
                FeatureFlag(GeneratedFeatureFlagKey.EMAIL_NOTIFICATION_DEFAULT, enabled = true),
                FeatureFlag(GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS, enabled = false),
                FeatureFlag(FeatureFlagKey.UseNotificationSenderForSystemNotifications, enabled = false),
                FeatureFlag(GeneratedFeatureFlagKey.USE_COMPOSE_FOR_MESSAGE_LIST_ITEMS, enabled = false),
                FeatureFlag(GeneratedFeatureFlagKey.MESSAGE_VIEW_ACTION_EXPORT_EML, enabled = true),
                FeatureFlag(GeneratedFeatureFlagKey.ENABLE_AVATAR_CUSTOMIZATION, enabled = true),
                // TODO(#10498): Remove when UseNewMessageReaderCssStyles is no longer required
                FeatureFlag(GeneratedFeatureFlagKey.USE_NEW_MESSAGE_READER_CSS_STYLES, enabled = true),
                FeatureFlag(GeneratedFeatureFlagKey.ENABLE_MESSAGE_LIST_NEW_STATE, enabled = false),
                FeatureFlag(GeneratedFeatureFlagKey.USE_COMPOSE_FOR_MESSAGE_READER, enabled = false),
                FeatureFlag(GeneratedFeatureFlagKey.THUNDERMAIL_ONBOARDING_ENABLED, enabled = true),
            ),
        )
    }
}
