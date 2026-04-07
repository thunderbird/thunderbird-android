package net.thunderbird.feature.mail.message.list.internal.ui.renderer.statepreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenPreviewParams
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenPreviewParamsProvider
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenRenderer
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessageListMetadataPreviewHelper
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreferencesPreviewHelper
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreviewHelper
import net.thunderbird.feature.mail.message.list.ui.component.rememberMessageListScope
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream

@OptIn(ExperimentalUuidApi::class)
private class MessageListScreenLoadingMessagesStatePreviewParamsProvider : MessageListScreenPreviewParamsProvider() {
    override val params = listOf(
        MessageListScreenPreviewParams(
            previewName = "Initial (0%)",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadingMessages(
                progress = 0f,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "Halfway (50%)",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadingMessages(
                progress = 0.5f,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "Pull To Refresh",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadingMessages(
                progress = 0f,
                isPullToRefresh = true,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "Remote Loading",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadingMessages(
                progress = 0.3f,
                isRemoteLoading = true,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
    )
}

@Composable
@PreviewLightDark
@PreviewScreenSizes
@PreviewLightDarkLandscape
private fun MessageListScreenLoadingMessagesStatePreview(
    @PreviewParameter(MessageListScreenLoadingMessagesStatePreviewParamsProvider::class)
    params: MessageListScreenPreviewParams,
) {
    val renderer = MessageListScreenRenderer()
    koinPreview {
        single<InAppNotificationStream> {
            object : InAppNotificationStream {
                override val notifications: StateFlow<Set<InAppNotification>> = MutableStateFlow(emptySet())
            }
        }
        single<NotificationIconResourceProvider> {
            object : NotificationIconResourceProvider {
                override val pushNotificationIcon: Int = 0
            }
        }
    } WithContent {
        ThunderbirdTheme2 {
            val scope = rememberMessageListScope()
            with(renderer) {
                scope.Render(
                    state = params.state,
                    dispatchEvent = {},
                )
            }
        }
    }
}
