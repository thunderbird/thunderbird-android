package net.thunderbird.feature.mail.message.list.ui.impl.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.impl.ui.dialog.CreateNewArchiveFolderDialogButtons
import net.thunderbird.feature.mail.message.list.impl.ui.dialog.CreateNewArchiveFolderDialogContent

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
private fun CreateNewArchiveFolderDialogContentPreview(
    @PreviewParameter(CreateArchiveFolderPreviewParamsCollection::class) params: CreateArchiveFolderPreviewParams,
) {
    PreviewWithThemesLightDark(
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
