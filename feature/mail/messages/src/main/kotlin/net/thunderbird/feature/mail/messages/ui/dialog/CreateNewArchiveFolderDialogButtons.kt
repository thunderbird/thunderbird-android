package net.thunderbird.feature.mail.messages.ui.dialog

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import net.thunderbird.feature.mail.messages.R

@Composable
internal fun RowScope.CreateNewArchiveFolderDialogButtons(
    isSynchronizing: Boolean,
    onCancelClick: () -> Unit,
    onCreateAndSetClick: () -> Unit,
) {
    TextButton(
        onClick = onCancelClick,
        enabled = isSynchronizing.not(),
    ) {
        TextLabelLarge(text = stringResource(R.string.setup_archive_folder_dialog_cancel))
    }
    TextButton(
        onClick = onCreateAndSetClick,
        enabled = isSynchronizing.not(),
    ) {
        TextLabelLarge(text = stringResource(R.string.setup_archive_folder_dialog_create_and_set_new_folder))
    }
}
