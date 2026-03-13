package net.thunderbird.feature.mail.message.list.ui.component.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import net.thunderbird.core.ui.compose.theme2.MainTheme

@PreviewLightDark
@Composable
private fun MessageBadgePreview() {
    PreviewWithThemesLightDark {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
            modifier = Modifier.padding(MainTheme.spacings.quadruple),
        ) {
            NewMessageBadge(modifier = Modifier.padding(MainTheme.spacings.double))
            UnreadMessageBadge(modifier = Modifier.padding(MainTheme.spacings.double))
        }
    }
}
