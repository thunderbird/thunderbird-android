package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.R

@PreviewLightDarkLandscape
@Composable
private fun EmailCantBeArchivedDialogButtonsPreview() {
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
            val state by remember {
                mutableStateOf(
                    SetupArchiveFolderDialogContract.State.EmailCantBeArchived(
                        isDoNotShowDialogAgainChecked = false,
                    ),
                )
            }
            Column(
                modifier = Modifier.padding(MainTheme.spacings.triple),
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
