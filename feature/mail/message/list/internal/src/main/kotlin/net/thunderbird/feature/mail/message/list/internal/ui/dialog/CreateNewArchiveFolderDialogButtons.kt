package net.thunderbird.feature.mail.message.list.internal.ui.dialog

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import net.thunderbird.feature.mail.message.list.internal.R

@Composable
internal fun RowScope.CreateNewArchiveFolderDialogButtons(
    isSynchronizing: Boolean,
    onCancelClick: () -> Unit,
    onCreateAndSetClick: () -> Unit,
) {
    ButtonText(
        onClick = onCancelClick,
        text = stringResource(R.string.setup_archive_folder_dialog_cancel),
        enabled = isSynchronizing.not(),
    )
    ButtonText(
        onClick = onCreateAndSetClick,
        text = stringResource(R.string.setup_archive_folder_dialog_create_and_set_new_folder),
        enabled = isSynchronizing.not(),
    )
}
