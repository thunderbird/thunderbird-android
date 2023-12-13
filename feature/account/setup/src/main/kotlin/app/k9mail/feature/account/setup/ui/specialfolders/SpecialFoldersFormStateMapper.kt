package app.k9mail.feature.account.setup.ui.specialfolders

import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormState

fun SpecialFolderOptions.toFormState(): FormState {
    return FormState(
        archiveSpecialFolderOptions = archiveSpecialFolderOptions,
        draftsSpecialFolderOptions = draftsSpecialFolderOptions,
        sentSpecialFolderOptions = sentSpecialFolderOptions,
        spamSpecialFolderOptions = spamSpecialFolderOptions,
        trashSpecialFolderOptions = trashSpecialFolderOptions,

        selectedArchiveSpecialFolderOption = archiveSpecialFolderOptions.first(),
        selectedDraftsSpecialFolderOption = draftsSpecialFolderOptions.first(),
        selectedSentSpecialFolderOption = sentSpecialFolderOptions.first(),
        selectedSpamSpecialFolderOption = spamSpecialFolderOptions.first(),
        selectedTrashSpecialFolderOption = trashSpecialFolderOptions.first(),
    )
}
