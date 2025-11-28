package net.thunderbird.feature.mail.message.list.impl.ui.dialog

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import net.thunderbird.feature.mail.message.list.api.ui.dialog.SetupArchiveFolderDialogContract.State
import net.thunderbird.feature.mail.message.list.impl.R

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
