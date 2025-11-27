package net.thunderbird.android.featureflag

import com.fsck.k9.ui.messageview.MessageViewFeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagFactory
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.toFeatureFlagKey
import net.thunderbird.feature.account.settings.AccountSettingsFeatureFlags
import net.thunderbird.feature.mail.message.list.MessageListFeatureFlags
import net.thunderbird.feature.mail.message.reader.api.MessageReaderFeatureFlags

/**
 * Feature flags for Thunderbird Beta
 */
class TbFeatureFlagFactory : FeatureFlagFactory {
    override fun getCatalog(): Flow<List<FeatureFlag>> = flow {
        emit(
            listOf(
                FeatureFlag("archive_marks_as_read".toFeatureFlagKey(), enabled = true),
                FeatureFlag("disable_font_size_config".toFeatureFlagKey(), enabled = true),
                FeatureFlag("email_notification_default".toFeatureFlagKey(), enabled = true),
                FeatureFlag(FeatureFlagKey.DisplayInAppNotifications, enabled = true),
                FeatureFlag(FeatureFlagKey.UseNotificationSenderForSystemNotifications, enabled = false),
                FeatureFlag(MessageListFeatureFlags.UseComposeForMessageListItems, enabled = false),
                FeatureFlag(MessageViewFeatureFlags.ActionExportEml, enabled = false),
                FeatureFlag(AccountSettingsFeatureFlags.EnableAvatarCustomization, enabled = false),
                FeatureFlag(MessageReaderFeatureFlags.UseNewMessageReaderCssStyles, enabled = false),
                FeatureFlag(MessageListFeatureFlags.EnableMessageListNewState, enabled = false),
            ),
        )
    }
}
