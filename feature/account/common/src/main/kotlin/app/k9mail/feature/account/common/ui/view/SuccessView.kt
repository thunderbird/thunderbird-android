package app.k9mail.feature.account.common.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun SuccessView(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MainTheme.spacings.default)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextTitleMedium(
            text = message,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.height(MainTheme.sizes.larger),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Celebration,
                tint = MainTheme.colors.secondary,
                modifier = Modifier.requiredSize(MainTheme.sizes.large),
            )
        }
    }
}
