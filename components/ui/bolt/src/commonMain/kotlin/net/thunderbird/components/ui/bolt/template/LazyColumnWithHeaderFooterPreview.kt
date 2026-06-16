package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
