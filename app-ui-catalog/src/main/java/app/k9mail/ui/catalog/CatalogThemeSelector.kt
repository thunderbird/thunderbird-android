package app.k9mail.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.theme.MainTheme

@Composable
fun CatalogThemeSelector(
    catalogTheme: CatalogTheme,
    modifier: Modifier = Modifier,
    onThemeChangeClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        TextBody1(text = "Change theme:")
        Button(
            text = catalogTheme.toString(),
            onClick = onThemeChangeClick,
        )
    }
}
