package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium

@Composable
@Preview(showBackground = true)
internal fun ContentListViewPreview() {
    PreviewWithThemes {
        ContentListView {
            item {
                TextTitleMedium("Item 1")
            }
            item {
                TextTitleMedium("Item 2")
            }
            item {
                TextTitleMedium("Item 3")
            }
        }
    }
}
