package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun AccountView(
    displayName: String,
    emailAddress: String,
    accountColor: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Max)
            .clickable(onClick = onClick)
            .padding(
                top = MainTheme.spacings.default,
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.triple,
                bottom = MainTheme.spacings.oneHalf,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountIndicator(
            accountColor = accountColor,
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    end = MainTheme.spacings.default,
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        ) {
            TextBodyLarge(
                text = displayName,
                color = MainTheme.colors.onSurface,
            )
            TextBodyMedium(
                text = emailAddress,
                color = MainTheme.colors.onSurfaceVariant,
            )
        }
    }
}
