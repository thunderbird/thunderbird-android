package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentLoadingPreview() {
    PreviewWithTheme {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = true,
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentFormPreview() {
    PreviewWithTheme {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = false,
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentSuccessPreview() {
    PreviewWithTheme {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = false,
                isSuccess = true,
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentErrorPreview() {
    PreviewWithTheme {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = false,
                error = SpecialFoldersContract.Failure.LoadFoldersFailed("Error"),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}
