package net.thunderbird.feature.mail.message.composer.dialog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.organism.BasicDialog
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.mail.message.composer.R

@Composable
fun SentFolderNotFoundConfirmationDialog(
    showDialog: Boolean,
    onAssignSentFolderClick: () -> Unit,
    onSendAndDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showDialog) {
        BasicDialog(
            headlineText = stringResource(R.string.sent_folder_not_found_dialog_title),
            supportingText = stringResource(R.string.sent_folder_not_found_dialog_supporting_text),
            content = {
                ButtonText(
                    onClick = onAssignSentFolderClick,
                    text = stringResource(R.string.sent_folder_not_found_dialog_assign_folder_action),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.padding(end = MainTheme.spacings.half),
                        )
                    },
                )
            },
            buttons = {
                ButtonText(
                    text = stringResource(R.string.sent_folder_not_found_dialog_cancel_action),
                    onClick = onDismiss,
                )
                ButtonText(
                    text = stringResource(R.string.sent_folder_not_found_dialog_send_and_delete_action),
                    onClick = onSendAndDeleteClick,
                    color = MainTheme.colors.error,
                )
            },
            onDismissRequest = onDismiss,
            contentPadding = PaddingValues(horizontal = MainTheme.spacings.default),
            modifier = modifier,
        )
    }
}
