package app.k9mail.ui.catalog.ui.common.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun DrawerCategoryItem(
    text: String,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .then(modifier),
    ) {
        TextSubtitle1(
            text = text,
            modifier = Modifier.padding(
                horizontal = MainTheme.spacings.double,
                vertical = MainTheme.spacings.default,
            ),
        )
    }
}

@Preview
@Composable
internal fun DrawerCategoryItemPreview() {
    PreviewWithThemes {
        DrawerCategoryItem(
            text = "Text",
            onItemClick = {},
        )
    }
}
