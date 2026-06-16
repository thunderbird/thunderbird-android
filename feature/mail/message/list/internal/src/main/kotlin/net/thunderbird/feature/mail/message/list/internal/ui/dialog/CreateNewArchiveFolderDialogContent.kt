package net.thunderbird.feature.mail.message.list.internal.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.feature.mail.message.list.internal.R

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
            syncingMessage?.let { message ->
                Spacer(modifier = Modifier.height(MainTheme.spacings.oneHalf))
                TextBodySmall(
                    text = message,
                    color = MainTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}
