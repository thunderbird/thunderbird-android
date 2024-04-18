package app.k9mail.feature.settings.push.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.settings.push.ui.PushFoldersContract.Effect
import app.k9mail.feature.settings.push.ui.PushFoldersContract.Event
import app.k9mail.feature.settings.push.ui.PushFoldersContract.State
import app.k9mail.feature.settings.push.ui.PushFoldersContract.ViewModel
import com.fsck.k9.Account.FolderMode

@Composable
@PreviewDevices
fun PushFoldersScreenPreview() {
    K9Theme {
        PushFoldersScreen(
            accountUuid = "accountUuid",
            onOptionSelected = {},
            onBack = {},
            viewModel = FakePushFolderViewModel(
                initialState = State(
                    isLoading = false,
                    showPermissionPrompt = true,
                    selectedOption = FolderMode.FIRST_CLASS,
                ),
            ),
        )
    }
}

private class FakePushFolderViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {
    override fun event(event: Event) = Unit
}
