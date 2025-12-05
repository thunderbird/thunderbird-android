package net.thunderbird.feature.mail.message.list.internal.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.internal.R
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.State

@Composable
internal fun EmailCantBeArchivedDialogButtons(
    state: State.EmailCantBeArchived,
    onSetArchiveFolderClick: () -> Unit,
    onSkipClick: () -> Unit,
    onDoNotShowAgainChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.oneHalf),
    ) {
        Box(
            modifier = Modifier.align(Alignment.End),
        ) {
            Row {
                ButtonText(
                    text = stringResource(R.string.setup_archive_folder_dialog_skip_for_now),
                    onClick = onSkipClick,
                )
                ButtonText(
                    text = stringResource(R.string.setup_archive_folder_dialog_set_archive_folder),
                    onClick = onSetArchiveFolderClick,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.Start)
                .toggleable(
                    value = state.isDoNotShowDialogAgainChecked,
                    role = Role.Checkbox,
                    onValueChange = { onDoNotShowAgainChange(!state.isDoNotShowDialogAgainChecked) },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        ) {
            Checkbox(
                checked = state.isDoNotShowDialogAgainChecked,
                onCheckedChange = null,
            )
            TextLabelSmall(text = stringResource(R.string.setup_archive_folder_dialog_dont_show_again))
        }
    }
}
