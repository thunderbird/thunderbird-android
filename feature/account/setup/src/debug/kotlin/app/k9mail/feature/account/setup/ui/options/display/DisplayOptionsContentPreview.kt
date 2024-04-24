package app.k9mail.feature.account.setup.ui.options.display

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun DisplayOptionsContentPreview() {
    PreviewWithTheme {
        DisplayOptionsContent(
            state = DisplayOptionsContract.State(),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}
