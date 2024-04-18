package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

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
