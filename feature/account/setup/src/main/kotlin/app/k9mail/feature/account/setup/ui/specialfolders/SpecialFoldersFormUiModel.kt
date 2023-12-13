package app.k9mail.feature.account.setup.ui.specialfolders

import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormEvent
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormState
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormUiModel

class SpecialFoldersFormUiModel : FormUiModel {

    override fun event(event: FormEvent, formState: FormState): FormState {
        return when (event) {
            is FormEvent.ArchiveFolderChanged -> onArchiveFolderChanged(formState, event.specialFolderOption)
            is FormEvent.DraftsFolderChanged -> onDraftsFolderChanged(formState, event.specialFolderOption)
            is FormEvent.SentFolderChanged -> onSentFolderChanged(formState, event.specialFolderOption)
            is FormEvent.SpamFolderChanged -> onSpamFolderChanged(formState, event.specialFolderOption)
            is FormEvent.TrashFolderChanged -> onTrashFolderChanged(formState, event.specialFolderOption)
        }
    }

    private fun onArchiveFolderChanged(formState: FormState, specialFolderOption: SpecialFolderOption): FormState {
        return formState.copy(
            selectedArchiveSpecialFolderOption = specialFolderOption,
        )
    }

    private fun onDraftsFolderChanged(formState: FormState, specialFolderOption: SpecialFolderOption): FormState {
        return formState.copy(
            selectedDraftsSpecialFolderOption = specialFolderOption,
        )
    }

    private fun onSentFolderChanged(formState: FormState, specialFolderOption: SpecialFolderOption): FormState {
        return formState.copy(
            selectedSentSpecialFolderOption = specialFolderOption,
        )
    }

    private fun onSpamFolderChanged(formState: FormState, specialFolderOption: SpecialFolderOption): FormState {
        return formState.copy(
            selectedSpamSpecialFolderOption = specialFolderOption,
        )
    }

    private fun onTrashFolderChanged(formState: FormState, specialFolderOption: SpecialFolderOption): FormState {
        return formState.copy(
            selectedTrashSpecialFolderOption = specialFolderOption,
        )
    }
}
