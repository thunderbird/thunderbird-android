package app.k9mail.ui.catalog.ui.common.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.ui.CatalogContract.Theme

@Composable
fun ThemeSelector(
    theme: Theme,
    modifier: Modifier = Modifier,
    onThemeChangeClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        TextBody1(text = "Change theme:")
        ButtonFilled(
            text = theme.toString(),
            onClick = onThemeChangeClick,
        )
    }
}
