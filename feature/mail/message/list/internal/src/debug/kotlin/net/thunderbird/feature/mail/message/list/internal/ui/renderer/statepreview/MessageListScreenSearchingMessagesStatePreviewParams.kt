package net.thunderbird.feature.mail.message.list.internal.ui.renderer.statepreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenPreviewParams
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenPreviewParamsProvider
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenRenderer
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessageListMetadataPreviewHelper
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreferencesPreviewHelper
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreviewHelper
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream

@OptIn(ExperimentalUuidApi::class)
private class MessageListScreenSearchingMessagesStatePreviewParamsProvider : MessageListScreenPreviewParamsProvider() {
    override val params = listOf(
        MessageListScreenPreviewParams(
            previewName = "SearchingMessages - Local Search",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.SearchingMessages(
                searchQuery = "invoice",
                isServerSearch = false,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = persistentListOf(MessagePreviewHelper.sampleMessages[2]),
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "SearchingMessages - Server Search",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.SearchingMessages(
                searchQuery = "project proposal",
                isServerSearch = true,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = persistentListOf(MessagePreviewHelper.sampleMessages[1]),
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "SearchingMessages - No Results",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.SearchingMessages(
                searchQuery = "nonexistent query",
                isServerSearch = false,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = persistentListOf(),
            ),
        ),
    )
}

@Composable
@PreviewLightDark
@PreviewScreenSizes
@PreviewLightDarkLandscape
private fun MessageListScreenSearchingMessagesStatePreview(
    @PreviewParameter(MessageListScreenSearchingMessagesStatePreviewParamsProvider::class)
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
            renderer.Render(
                state = params.state,
                dispatchEvent = {},
                onEffect = {},
            )
        }
    }
}
