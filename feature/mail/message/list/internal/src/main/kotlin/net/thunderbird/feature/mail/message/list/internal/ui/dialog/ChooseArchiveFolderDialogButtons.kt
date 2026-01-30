package net.thunderbird.feature.mail.message.list.internal.ui.dialog

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import net.thunderbird.feature.mail.message.list.internal.R
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.State

@Composable
internal fun RowScope.ChooseArchiveFolderDialogButtons(
    state: State.ChooseArchiveFolder,
    onCreateNewFolderClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    ButtonText(
        text = stringResource(R.string.setup_archive_folder_dialog_create_new_folder),
        onClick = onCreateNewFolderClick,
    )
    ButtonText(
        text = stringResource(R.string.setup_archive_folder_dialog_done),
        enabled = state.isLoadingFolders.not(),
        onClick = onDoneClick,
    )
}
