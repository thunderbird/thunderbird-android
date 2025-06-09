package net.thunderbird.feature.mail.messages.ui.dialog

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import net.thunderbird.feature.mail.messages.R
import net.thunderbird.feature.mail.messages.ui.dialog.SetupArchiveFolderDialogContract.State

@Composable
internal fun RowScope.ChooseArchiveFolderDialogButtons(
    state: State.ChooseArchiveFolder,
    onCreateNewFolderClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    TextButton(
        onClick = onCreateNewFolderClick,
    ) {
        TextLabelLarge(text = stringResource(R.string.setup_archive_folder_dialog_create_new_folder))
    }
    TextButton(
        enabled = state.isLoadingFolders.not(),
        onClick = onDoneClick,
    ) {
        TextLabelLarge(text = stringResource(R.string.setup_archive_folder_dialog_done))
    }
}
