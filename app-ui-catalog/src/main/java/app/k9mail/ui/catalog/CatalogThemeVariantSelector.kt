package app.k9mail.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.theme.MainTheme

@Composable
fun CatalogThemeVariantSelector(
    catalogThemeVariant: CatalogThemeVariant,
    modifier: Modifier = Modifier,
    onThemeVariantChange: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        TextBody1(text = "Dark mode:")
        Checkbox(
            checked = catalogThemeVariant == CatalogThemeVariant.DARK,
            onCheckedChange = { onThemeVariantChange() },
        )
    }
}
