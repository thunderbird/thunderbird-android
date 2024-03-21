package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
internal fun DividerHorizontalPreview() {
    PreviewWithThemes {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MainTheme.spacings.double),
        ) {
            DividerHorizontal(
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
