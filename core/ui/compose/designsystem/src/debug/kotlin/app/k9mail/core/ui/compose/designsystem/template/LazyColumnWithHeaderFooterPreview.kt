package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
internal fun LazyColumnWithHeaderFooterPreview() {
    PreviewWithTheme {
        Surface {
            LazyColumnWithHeaderFooter(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double, Alignment.CenterVertically),
                header = { TextTitleMedium(text = "Header") },
                footer = { TextTitleMedium(text = "Footer") },
            ) {
                items(10) {
                    TextBodyLarge(text = "Item $it")
                }
            }
        }
    }
}
