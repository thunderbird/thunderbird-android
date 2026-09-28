package net.thunderbird.feature.mail.message.reader.api.ui.bridge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableSet
import net.thunderbird.feature.mail.message.reader.api.domain.MessageReaderAction

fun interface MessageReaderBottomSheet {
    @Composable
    fun Content(
        actions: ImmutableSet<MessageReaderAction>,
        onClick: (action: MessageReaderAction) -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier,
    )
}
