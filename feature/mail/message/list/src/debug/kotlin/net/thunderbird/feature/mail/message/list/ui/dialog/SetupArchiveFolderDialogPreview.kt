package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.State

private class SetupArchiveFolderDialogParamCol : CollectionPreviewParameterProvider<State>(
    setOf(
        State.EmailCantBeArchived(isDoNotShowDialogAgainChecked = true),
        State.EmailCantBeArchived(isDoNotShowDialogAgainChecked = false),
        State.ChooseArchiveFolder(isLoadingFolders = false, folders = emptyList()),
        State.ChooseArchiveFolder(isLoadingFolders = true, folders = emptyList()),
        State.ChooseArchiveFolder(
            isLoadingFolders = false,
            folders = List(size = 5) {
                RemoteFolder(
                    id = it.toLong(),
                    serverId = "$it",
                    name = "Folder 1",
                    type = FolderType.REGULAR,
                )
            },
        ),
        State.CreateArchiveFolder(syncingMessage = null, folderName = ""),
        State.CreateArchiveFolder(syncingMessage = "any message", folderName = ""),
    ),
)

@PreviewLightDark
@Composable
private fun SetupArchiveFolderDialogPreview(
    @PreviewParameter(SetupArchiveFolderDialogParamCol::class) state: State,
) {
    ThunderbirdTheme2 {
        Surface(modifier = Modifier.fillMaxSize()) {
            SetupArchiveFolderDialog(state = state)
        }
    }
}
