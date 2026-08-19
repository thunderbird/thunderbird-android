package app.k9mail.featureflag

import com.fsck.k9.ui.messageview.MessageViewFeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.featureflag.toFeatureFlagKey
import net.thunderbird.feature.mail.message.reader.api.MessageReaderFeatureFlags
import net.thunderbird.feature.thundermail.featureflag.ThundermailFeatureFlags

/**
 * Feature flags for K-9 Mail (release)
 */
class K9FeatureFlagFactory : FeatureFlagFactory {
    override fun getCatalog(): Flow<List<FeatureFlag>> = flow {
        emit(
            listOf(
                FeatureFlag("archive_marks_as_read".toFeatureFlagKey(), enabled = true),
                FeatureFlag("disable_font_size_config".toFeatureFlagKey(), enabled = true),
                FeatureFlag("email_notification_default".toFeatureFlagKey(), enabled = true),
                FeatureFlag(FeatureFlagKey.DisplayInAppNotifications, enabled = false),
                FeatureFlag(FeatureFlagKey.UseNotificationSenderForSystemNotifications, enabled = false),
                FeatureFlag(GeneratedFeatureFlagKey.USE_COMPOSE_FOR_MESSAGE_LIST_ITEMS, enabled = false),
                FeatureFlag(MessageViewFeatureFlags.ActionExportEml, enabled = false),
                FeatureFlag(GeneratedFeatureFlagKey.ENABLE_AVATAR_CUSTOMIZATION, enabled = true),
                // TODO(#10498): Clean up all the legacy code that is wrapped with UseNewMessageReaderCssStyles
                //  once it no longer required
                FeatureFlag(GeneratedFeatureFlagKey.USE_NEW_MESSAGE_READER_CSS_STYLES, enabled = true),
                FeatureFlag(GeneratedFeatureFlagKey.ENABLE_MESSAGE_LIST_NEW_STATE, enabled = false),
                FeatureFlag(MessageReaderFeatureFlags.UseComposeForMessageReader, enabled = false),
                FeatureFlag(ThundermailFeatureFlags.ThundermailOnboardingEnabled, enabled = true),
            ),
        )
    }
}
