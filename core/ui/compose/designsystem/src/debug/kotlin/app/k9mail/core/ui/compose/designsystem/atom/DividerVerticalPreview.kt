package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
internal fun DividerVerticalPreview() {
    PreviewWithThemes {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(MainTheme.spacings.double),
        ) {
            DividerVertical(
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}
