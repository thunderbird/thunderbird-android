package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessageListMetadataPreviewHelper
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreferencesPreviewHelper
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreviewHelper
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.MessageListFooter
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream

// region [ ------ Preview Parameters ------ ]
private data class MessageListScreenPreviewParams(
    val previewName: String,
    val preferences: MessageListPreferences,
    val state: MessageListState,
)

@OptIn(ExperimentalUuidApi::class)
private class MessageListScreenPreviewParamsProvider : PreviewParameterProvider<MessageListScreenPreviewParams> {
    private val _values = listOf(
        // region [ WarmingUp ]
        MessageListScreenPreviewParams(
            previewName = "WarmingUp - Initial State",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.WarmingUp(),
        ),
        // endregion

        // region [ LoadingMessages ]
        MessageListScreenPreviewParams(
            previewName = "LoadingMessages - Initial (0%)",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadingMessages(
                progress = 0f,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadingMessages - Halfway (50%)",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadingMessages(
                progress = 0.5f,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadingMessages - Pull To Refresh",
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
            previewName = "LoadingMessages - Remote Loading",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadingMessages(
                progress = 0.3f,
                isRemoteLoading = true,
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        // endregion

        // region [ LoadedMessages ]
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Default Density",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Compact Density",
            preferences = MessagePreferencesPreviewHelper.compactPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.compactPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Relaxed Density",
            preferences = MessagePreferencesPreviewHelper.relaxedPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.relaxedPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Sender Above Subject",
            preferences = MessagePreferencesPreviewHelper.senderAboveSubjectPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.senderAboveSubjectPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - No Avatar, No Favourite",
            preferences = MessagePreferencesPreviewHelper.noAvatarNoFavouritePreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.noAvatarNoFavouritePreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Full Date Format",
            preferences = MessagePreferencesPreviewHelper.fullDatePreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.fullDatePreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Empty Inbox",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = persistentListOf(),
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Sent Folder",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.sentMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - With Conversations",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.conversationMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Active Message",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata.copy(
                    activeMessage = MessagePreviewHelper.sampleMessages.first(),
                    isActive = true,
                ),
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Multi Account",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.defaultMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.multiAccountMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - With Footer",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata.copy(
                    footer = MessageListFooter(showFooter = true, text = "Load more messages on server"),
                ),
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        MessageListScreenPreviewParams(
            previewName = "LoadedMessages - Without Footer",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.LoadedMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata.copy(
                    footer = MessageListFooter(showFooter = false),
                ),
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.sampleMessages,
            ),
        ),
        // endregion

        // region [ SearchingMessages ]
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
        // endregion

        // region [ SelectingMessages ]
        MessageListScreenPreviewParams(
            previewName = "SelectingMessages - Two Selected",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.SelectingMessages(
                metadata = MessageListMetadataPreviewHelper.inboxMetadata,
                preferences = MessagePreferencesPreviewHelper.defaultPreferences,
                messages = MessagePreviewHelper.selectedMessages,
            ),
        ),
        // endregion
    )

    override val values: Sequence<MessageListScreenPreviewParams> = _values.asSequence()

    override fun getDisplayName(index: Int): String = _values[index].previewName
}
// endregion [ ------ Preview Parameters ------ ]

// region [ ------ Preview ------ ]
@Composable
@PreviewLightDark
@PreviewScreenSizes
@PreviewLightDarkLandscape
private fun Preview(
    @PreviewParameter(MessageListScreenPreviewParamsProvider::class) params: MessageListScreenPreviewParams,
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
                preferences = params.preferences,
            )
        }
    }
}
// endregion [ ------ Preview ------ ]
