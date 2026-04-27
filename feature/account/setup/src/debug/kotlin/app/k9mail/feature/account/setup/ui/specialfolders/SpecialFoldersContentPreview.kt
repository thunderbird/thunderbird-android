package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentLoadingPreview() {
    ThundermailPreview {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = true,
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
            maxWidth = Dp.Unspecified,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentFormPreview() {
    ThundermailPreview {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = false,
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
            maxWidth = Dp.Unspecified,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentSuccessPreview() {
    ThundermailPreview {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = false,
                isSuccess = true,
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
            maxWidth = Dp.Unspecified,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersContentErrorPreview() {
    ThundermailPreview {
        SpecialFoldersContent(
            state = SpecialFoldersContract.State(
                isLoading = false,
                error = SpecialFoldersContract.Failure.LoadFoldersFailed("Error"),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
            maxWidth = Dp.Unspecified,
        )
    }
}
