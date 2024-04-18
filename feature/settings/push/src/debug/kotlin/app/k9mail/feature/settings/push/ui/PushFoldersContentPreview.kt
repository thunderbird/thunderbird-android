package app.k9mail.feature.settings.push.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.settings.push.ui.PushFoldersContract.State
import com.fsck.k9.Account.FolderMode

@Composable
@Preview(showBackground = true)
fun PushFoldersContentWithPermissionPromptPreview() {
    K9Theme {
        PushFoldersContent(
            state = State(
                isLoading = false,
                showPermissionPrompt = true,
                selectedOption = FolderMode.NONE,
            ),
            onEvent = {},
            innerPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun PushFoldersContentWithoutPermissionPromptPreview() {
    K9Theme {
        PushFoldersContent(
            state = State(
                isLoading = false,
                showPermissionPrompt = false,
                selectedOption = FolderMode.ALL,
            ),
            onEvent = {},
            innerPadding = PaddingValues(),
        )
    }
}
