package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplaySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import net.thunderbird.core.ui.compose.theme2.MainTheme

@PreviewLightDark
@Composable
private fun Preview() {
    PreviewWithThemesLightDark {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(MainTheme.spacings.triple),
        ) {
            TextDisplaySmall("Message Counter Preview:")
            TextHeadlineSmall("New Message: ")
            Row(horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default)) {
                NewMessageConversationCounterBadge(count = 1)
                NewMessageConversationCounterBadge(count = 7)
                NewMessageConversationCounterBadge(count = 10)
                NewMessageConversationCounterBadge(count = 25)
                NewMessageConversationCounterBadge(count = 100)
            }
            TextHeadlineSmall("Read Message: ")
            Row(horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default)) {
                ReadMessageConversationCounterBadge(count = 1)
                ReadMessageConversationCounterBadge(count = 7)
                ReadMessageConversationCounterBadge(count = 10)
                ReadMessageConversationCounterBadge(count = 25)
                ReadMessageConversationCounterBadge(count = 100)
            }
            TextHeadlineSmall("Unread Message: ")
            Row(horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default)) {
                UnreadMessageConversationCounterBadge(count = 1)
                UnreadMessageConversationCounterBadge(count = 7)
                UnreadMessageConversationCounterBadge(count = 10)
                UnreadMessageConversationCounterBadge(count = 25)
                UnreadMessageConversationCounterBadge(count = 100)
            }
        }
    }
}
