package net.thunderbird.feature.messages.ui.dialog

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.messages.R
import net.thunderbird.feature.messages.ui.dialog.SetupArchiveFolderDialogContract.State

@Composable
internal fun ChooseArchiveFolderDialogContent(
    state: State.ChooseArchiveFolder,
    onFolderSelect: (RemoteFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = state.isLoadingFolders,
        modifier = modifier.fillMaxWidth(),
    ) { isLoading ->
        when {
            isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MainTheme.spacings.triple),
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage?.isNotBlank() == true -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MainTheme.spacings.triple),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        tint = MainTheme.colors.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    TextBodyMedium(
                        text = stringResource(R.string.setup_archive_folder_dialog_error_retrieve_folders),
                    )
                    TextBodyMedium(
                        text = stringResource(
                            R.string.setup_archive_folder_dialog_error_retrieve_folders_detailed_message,
                            state.errorMessage,
                        ),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                ) {
                    items(items = state.folders) { folder ->
                        RemoteFolderListItem(
                            folderName = folder.name,
                            isSelected = state.selectedFolder == folder,
                            onFolderSelect = { onFolderSelect(folder) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RowScope.ChooseArchiveFolderDialogButtons(
    state: State.ChooseArchiveFolder,
    onCreateNewFolderClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    TextButton(
        onClick = onCreateNewFolderClick,
    ) {
        Text(text = stringResource(R.string.setup_archive_folder_dialog_create_new_folder))
    }
    TextButton(
        enabled = state.isLoadingFolders.not(),
        onClick = onDoneClick,
    ) {
        Text(text = stringResource(R.string.setup_archive_folder_dialog_done))
    }
}

@Composable
private fun RemoteFolderListItem(
    folderName: String,
    isSelected: Boolean,
    onFolderSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onFolderSelect,
            )
            .padding(horizontal = MainTheme.spacings.oneHalf),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onFolderSelect,
        )
        TextBodyMedium(text = folderName)
    }
}

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
private fun Preview(
    @PreviewParameter(ChooseArchiveFolderDialogContentParamsCol::class) state: State.ChooseArchiveFolder,
) {
    PreviewWithThemeLightDark(
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
