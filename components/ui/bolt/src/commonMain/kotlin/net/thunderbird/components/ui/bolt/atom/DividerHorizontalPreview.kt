package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
