package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextCaption
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormEvent
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormState
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongMethod")
@Composable
fun SpecialFoldersFormContent(
    state: FormState,
    onEvent: (FormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    LazyColumn(
        modifier = Modifier
            .imePadding()
            .then(modifier),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
    ) {
        item {
            Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
        }

        item {
            TextBodyLarge(
                text = stringResource(id = R.string.account_setup_special_folders_form_description),
                modifier = Modifier.padding(defaultItemPadding()),
            )
        }

        item {
            SelectInput(
                options = state.archiveSpecialFolderOptions.toImmutableList(),
                selectedOption = state.selectedArchiveSpecialFolderOption,
                onOptionChange = { onEvent(FormEvent.ArchiveFolderChanged(it)) },
                optionToStringTransformation = { it.toResourceString(resources) },
                label = stringResource(R.string.account_setup_special_folders_archive_folder_label),
                contentPadding = defaultItemPadding(),
            )
        }

        item {
            SelectInput(
                options = state.draftsSpecialFolderOptions.toImmutableList(),
                selectedOption = state.selectedDraftsSpecialFolderOption,
                onOptionChange = { onEvent(FormEvent.DraftsFolderChanged(it)) },
                optionToStringTransformation = { it.toResourceString(resources) },
                label = stringResource(id = R.string.account_setup_special_folders_drafts_folder_label),
                contentPadding = defaultItemPadding(),
            )
        }

        item {
            SelectInput(
                options = state.sentSpecialFolderOptions.toImmutableList(),
                selectedOption = state.selectedSentSpecialFolderOption,
                onOptionChange = { onEvent(FormEvent.SentFolderChanged(it)) },
                optionToStringTransformation = { it.toResourceString(resources) },
                label = stringResource(id = R.string.account_setup_special_folders_sent_folder_label),
                contentPadding = defaultItemPadding(),
            )
        }

        item {
            SelectInput(
                options = state.spamSpecialFolderOptions.toImmutableList(),
                selectedOption = state.selectedSpamSpecialFolderOption,
                onOptionChange = { onEvent(FormEvent.SpamFolderChanged(it)) },
                optionToStringTransformation = { it.toResourceString(resources) },
                label = stringResource(id = R.string.account_setup_special_folders_spam_folder_label),
                contentPadding = defaultItemPadding(),
            )
        }

        item {
            SelectInput(
                options = state.trashSpecialFolderOptions.toImmutableList(),
                selectedOption = state.selectedTrashSpecialFolderOption,
                onOptionChange = { onEvent(FormEvent.TrashFolderChanged(it)) },
                optionToStringTransformation = { it.toResourceString(resources) },
                label = stringResource(id = R.string.account_setup_special_folders_trash_folder_label),
                contentPadding = defaultItemPadding(),
            )
        }

        item {
            TextCaption(
                text = stringResource(id = R.string.account_setup_special_folders_form_description_automatic),
                modifier = Modifier.padding(defaultItemPadding()),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun SpecialFoldersFormContentPreview() {
    PreviewWithThemes {
        SpecialFoldersFormContent(
            state = FormState(),
            onEvent = {},
        )
    }
}
