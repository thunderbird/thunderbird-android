package app.k9mail.feature.account.edit.ui.server.settings.save

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun SaveServerSettingsContentPreview() {
    PreviewWithThemes {
        SaveServerSettingsContent(
            state = SaveServerSettingsContract.State(
                isLoading = false,
                error = null,
            ),
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SaveServerSettingsContentLoadingPreview() {
    PreviewWithThemes {
        SaveServerSettingsContent(
            state = SaveServerSettingsContract.State(
                isLoading = true,
                error = null,
            ),

            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SaveServerSettingsContentErrorPreview() {
    PreviewWithThemes {
        SaveServerSettingsContent(
            state = SaveServerSettingsContract.State(
                isLoading = false,
                error = SaveServerSettingsContract.Failure.SaveServerSettingsFailed(
                    message = "Error",
                ),
            ),
            contentPadding = PaddingValues(),
        )
    }
}
