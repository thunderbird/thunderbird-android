package net.thunderbird.feature.mail.message.list.internal.ui.component.organism

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val FOOTER_HEIGHT = 64

@Composable
internal fun MessageListFooter(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FOOTER_HEIGHT.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonText(
            text = state.metadata.footer.text,
            onClick = { dispatchEvent(MessageListEvent.OnFooterClick) },
        )
    }
}
