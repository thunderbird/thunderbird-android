package net.thunderbird.feature.mail.message.list.ui.impl.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.api.ui.dialog.SetupArchiveFolderDialogContract.State
import net.thunderbird.feature.mail.message.list.impl.ui.dialog.ChooseArchiveFolderDialogButtons
import net.thunderbird.feature.mail.message.list.impl.ui.dialog.ChooseArchiveFolderDialogContent

private class ChooseArchiveFolderDialogContentParamsCol :
    CollectionPreviewParameterProvider<State.ChooseArchiveFolder>(
        setOf(
            State.ChooseArchiveFolder(
                isLoadingFolders = true,
            ),
            State.ChooseArchiveFolder(
                isLoadingFolders = false,
                folders = listOf(
                    RemoteFolder(
                        id = 1,
                        serverId = "1",
                        name = "[Gmail]/All Mail",
                        type = FolderType.REGULAR,
                    ),
                    RemoteFolder(id = 2, serverId = "2", name = "[Gmail]/Draft", type = FolderType.REGULAR),
                    RemoteFolder(
                        id = 3,
                        serverId = "3",
                        name = "[Gmail]/Sent Mail",
                        type = FolderType.REGULAR,
                    ),
                    RemoteFolder(id = 3, serverId = "3", name = "[Gmail]/Spam", type = FolderType.REGULAR),
                    RemoteFolder(id = 3, serverId = "3", name = "[Gmail]/Trash", type = FolderType.REGULAR),
                    RemoteFolder(
                        id = 3,
                        serverId = "3",
                        name = "[Gmail]/Another Folder",
                        type = FolderType.REGULAR,
                    ),
                ),
            ),
            State.ChooseArchiveFolder(
                isLoadingFolders = false,
                errorMessage = "Error message",
            ),
        ),
    )

@PreviewLightDarkLandscape
@Composable
private fun ChooseArchiveFolderDialogContentPreview(
    @PreviewParameter(ChooseArchiveFolderDialogContentParamsCol::class) state: State.ChooseArchiveFolder,
) {
    PreviewWithThemesLightDark(
        useRow = true,
        useScrim = true,
        scrimPadding = PaddingValues(32.dp),
        arrangement = Arrangement.spacedBy(24.dp),
    ) {
        Surface(
            shape = MainTheme.shapes.extraLarge,
            modifier = Modifier.width(300.dp),
        ) {
            Column {
                ChooseArchiveFolderDialogContent(
                    state = state,
                    onFolderSelect = {},
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ChooseArchiveFolderDialogButtons(
                        state = state,
                        onCreateNewFolderClick = {},
                        onDoneClick = {},
                    )
                }
            }
        }
    }
}
