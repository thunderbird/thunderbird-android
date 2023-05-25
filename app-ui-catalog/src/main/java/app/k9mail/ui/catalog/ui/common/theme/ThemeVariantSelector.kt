package app.k9mail.ui.catalog.ui.common.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant

@Composable
fun ThemeVariantSelector(
    themeVariant: ThemeVariant,
    modifier: Modifier = Modifier,
    onThemeVariantChange: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        TextBody1(text = "Enable dark mode:")
        Checkbox(
            checked = themeVariant == ThemeVariant.DARK,
            onCheckedChange = { onThemeVariantChange() },
        )
    }
}
