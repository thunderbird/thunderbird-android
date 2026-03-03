package net.thunderbird.feature.mail.message.list.internal.ui.renderer.statepreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenPreviewParams
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenPreviewParamsProvider
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenRenderer
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreferencesPreviewHelper
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream

private class MessageListScreenWarmingUpStatePreviewParamsProvider : MessageListScreenPreviewParamsProvider() {
    override val params = listOf(
        MessageListScreenPreviewParams(
            previewName = "WarmingUp - Initial State",
            preferences = MessagePreferencesPreviewHelper.defaultPreferences,
            state = MessageListState.WarmingUp(),
        ),
    )
}

@Composable
@PreviewLightDark
@PreviewScreenSizes
@PreviewLightDarkLandscape
private fun MessageListScreenWarmingUpStatePreview(
    @PreviewParameter(MessageListScreenWarmingUpStatePreviewParamsProvider::class)
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
                preferences = params.preferences,
            )
        }
    }
}
