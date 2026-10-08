package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

data class MessageListScreenPreviewParams(
    val previewName: String,
    val preferences: MessageListPreferences,
    val state: MessageListState,
)

abstract class MessageListScreenPreviewParamsProvider : PreviewParameterProvider<MessageListScreenPreviewParams> {
    abstract val params: List<MessageListScreenPreviewParams>
    override val values: Sequence<MessageListScreenPreviewParams> get() = params.asSequence()
    override fun getDisplayName(index: Int): String = params[index].previewName
}
