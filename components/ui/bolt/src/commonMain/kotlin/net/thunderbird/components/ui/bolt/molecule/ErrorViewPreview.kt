package net.thunderbird.components.ui.bolt.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ErrorViewPreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ErrorViewWithMessagePreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            message = "Something went wrong.",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ErrorViewWithRetryPreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            onRetry = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ErrorViewWithRetryAndMessagePreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            message = "Something went wrong.",
            onRetry = {},
        )
    }
}
