package app.k9mail.ui.catalog.ui.common.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline6
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun DrawerHeaderItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MainTheme.spacings.double,
                top = MainTheme.spacings.double,
                end = MainTheme.spacings.double,
            )
            .then(modifier),
    ) {
        TextHeadline6(
            text = text,
        )
        DividerHorizontal()
    }
}

@Preview
@Composable
internal fun DrawerHeaderItemPreview() {
    PreviewWithThemes {
        DrawerHeaderItem(
            text = "Category",
        )
    }
}
