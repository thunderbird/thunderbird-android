package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.R

@Composable
internal fun EmailCantBeArchivedDialogButtons(
    state: SetupArchiveFolderDialogContract.State.EmailCantBeArchived,
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

@PreviewLightDarkLandscape
@Composable
private fun Preview() {
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
            val state by remember { mutableStateOf(SetupArchiveFolderDialogContract.State.EmailCantBeArchived(isDoNotShowDialogAgainChecked = false)) }
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                TextBodyMedium(text = stringResource(R.string.setup_archive_folder_dialog_configure_archive_folder))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    EmailCantBeArchivedDialogButtons(
                        state = state,
                        onSetArchiveFolderClick = {},
                        onSkipClick = {},
                        onDoNotShowAgainChange = {},
                    )
                }
            }
        }
    }
}
