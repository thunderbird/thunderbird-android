package net.thunderbird.feature.mail.message.list.ui.component.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.theme.MainTheme

@PreviewLightDark
@Composable
private fun MessageBadgePreview() {
    PreviewWithThemesLightDark {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
            modifier = Modifier.padding(MainTheme.spacings.quadruple),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NewMessageBadge(modifier = Modifier.padding(MainTheme.spacings.double))
            UnreadMessageBadge(modifier = Modifier.padding(MainTheme.spacings.double))
        }
    }
}
