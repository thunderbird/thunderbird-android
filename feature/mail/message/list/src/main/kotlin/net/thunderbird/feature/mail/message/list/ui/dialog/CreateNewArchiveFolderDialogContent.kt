package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.R

@Composable
internal fun CreateNewArchiveFolderDialogContent(
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    syncingMessage: String? = null,
    errorMessage: String? = null,
) {
    Column(
        modifier = modifier
            .padding(horizontal = MainTheme.spacings.oneHalf),
    ) {
        TextInput(
            onTextChange = onFolderNameChange,
            text = folderName,
            label = stringResource(R.string.setup_archive_folder_dialog_create_new_folder),
            isEnabled = syncingMessage.isNullOrEmpty(),
            errorMessage = errorMessage,
        )

        AnimatedVisibility(
            visible = syncingMessage != null,
            modifier = Modifier.padding(horizontal = MainTheme.spacings.quadruple),
        ) {
            requireNotNull(syncingMessage)

            Spacer(modifier = Modifier.height(MainTheme.spacings.oneHalf))
            TextBodySmall(
                text = syncingMessage,
                color = MainTheme.colors.onSurfaceVariant,
            )
        }
    }
}

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

private data class CreateArchiveFolderPreviewParams(
    val folderName: String,
    val synchronizingMessage: String? = null,
    val errorMessage: String? = null,
)

private class CreateArchiveFolderPreviewParamsCollection :
    CollectionPreviewParameterProvider<CreateArchiveFolderPreviewParams>(
        setOf(
            CreateArchiveFolderPreviewParams(
                folderName = "",
                synchronizingMessage = null,
            ),
            CreateArchiveFolderPreviewParams(
                folderName = "My new awesome folder",
                synchronizingMessage = null,
            ),
            CreateArchiveFolderPreviewParams(
                folderName = "A ${"very ".repeat(n = 100)} long folder name",
                synchronizingMessage = null,
            ),
            CreateArchiveFolderPreviewParams(
                folderName = "",
                synchronizingMessage = "Preparing sync",
            ),
            CreateArchiveFolderPreviewParams(
                folderName = "My new awesome folder",
                synchronizingMessage = "Doing some sync stuff.",
            ),
            CreateArchiveFolderPreviewParams(
                folderName = "A ${"very ".repeat(n = 100)} long folder name",
                synchronizingMessage = "A ${"very ".repeat(n = 100)} long sync message",
            ),
            CreateArchiveFolderPreviewParams(
                folderName = "A ${"very ".repeat(n = 100)} long folder name",
                synchronizingMessage = "",
                errorMessage = "Can not create folder.",
            ),
            CreateArchiveFolderPreviewParams(
                folderName = "A ${"very ".repeat(n = 100)} long folder name",
                synchronizingMessage = null,
                errorMessage = "A ${"very ".repeat(n = 100)} long error message",
            ),
        ),
    )

@PreviewLightDarkLandscape
@Composable
private fun Preview(
    @PreviewParameter(CreateArchiveFolderPreviewParamsCollection::class) params: CreateArchiveFolderPreviewParams,
) {
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
            Column {
                CreateNewArchiveFolderDialogContent(
                    folderName = params.folderName,
                    syncingMessage = params.synchronizingMessage,
                    errorMessage = params.errorMessage,
                    onFolderNameChange = {},
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    CreateNewArchiveFolderDialogButtons(
                        isSynchronizing = params.synchronizingMessage?.isNotEmpty() == true,
                        onCreateAndSetClick = {},
                        onCancelClick = {},
                    )
                }
            }
        }
    }
}
