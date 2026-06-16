package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
