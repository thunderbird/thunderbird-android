package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.button.RadioButton
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.R
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.State

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
            label = { TextBodyMedium(text = folderName) },
            onClick = onFolderSelect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MainTheme.spacings.oneHalf),
        )
    }
}
