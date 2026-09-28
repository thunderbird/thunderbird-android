package net.thunderbird.feature.mail.message.reader.impl.ui.molecule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.organism.ModalBottomSheet
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.reader.api.domain.MessageReaderAction
import net.thunderbird.feature.mail.message.reader.api.ui.bridge.MessageReaderBottomSheet
import org.jetbrains.compose.resources.stringResource

object DefaultMessageReaderBottomSheet : MessageReaderBottomSheet {
    @Composable
    override fun Content(
        actions: ImmutableSet<MessageReaderAction>,
        onClick: (action: MessageReaderAction) -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier,
    ) {
        ModalBottomSheet(onDismissRequest = onDismiss, modifier) {
            MessageReaderBottomSheetContent(actions, onClick)
        }
    }
}

@Composable
private fun MessageReaderBottomSheetContent(
    actions: ImmutableSet<MessageReaderAction>,
    onClick: (action: MessageReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.half),
    ) {
        actions.forEach { action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onClick(action) }),
                horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = action.icon,
                    modifier = Modifier
                        .padding(vertical = BoltTheme.spacings.oneHalf)
                        .padding(start = BoltTheme.spacings.oneHalf),
                )
                TextTitleSmall(
                    text = stringResource(action.label),
                    modifier = Modifier
                        .padding(vertical = BoltTheme.spacings.oneHalf)
                        .padding(end = BoltTheme.spacings.double),
                )
            }
        }
    }
}

class MessageReaderBottomSheetPreviewParams : CollectionPreviewParameterProvider<ImmutableSet<MessageReaderAction>>(
    listOf(
        persistentSetOf(MessageReaderAction.Reply, MessageReaderAction.ReplyAll),
        MessageReaderAction.entries.toPersistentSet(),
        (MessageReaderAction.entries - MessageReaderAction.ReplyAll).toPersistentSet(),
    ),
)

@PreviewLightDark
@Composable
private fun Preview(
    @PreviewParameter(MessageReaderBottomSheetPreviewParams::class) actions: ImmutableSet<MessageReaderAction>,
) {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(BoltTheme.spacings.quadruple)) {
            MessageReaderBottomSheetContent(
                actions = actions,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
