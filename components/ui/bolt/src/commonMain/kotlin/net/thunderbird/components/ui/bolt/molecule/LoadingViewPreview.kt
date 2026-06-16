package net.thunderbird.components.ui.bolt.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun LoadingViewPreview() {
    PreviewWithThemes {
        LoadingView()
    }
}

@Composable
@Preview(showBackground = true)
internal fun LoadingViewWithMessagePreview() {
    PreviewWithThemes {
        LoadingView(
            message = "Loading ...",
        )
    }
}
