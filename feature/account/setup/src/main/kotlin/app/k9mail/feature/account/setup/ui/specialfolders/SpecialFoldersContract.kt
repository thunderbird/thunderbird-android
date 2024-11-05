package app.k9mail.feature.account.setup.ui.specialfolders

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingErrorState
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption

interface SpecialFoldersContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    interface FormUiModel {
        fun event(event: FormEvent, formState: FormState): FormState
    }

    data class State(
        val formState: FormState = FormState(),

        val isManualSetup: Boolean = false,
        val isSuccess: Boolean = false,
        override val error: Failure? = null,
        override val isLoading: Boolean = true,
    ) : LoadingErrorState<Failure>

    data class FormState(
        val archiveSpecialFolderOptions: List<SpecialFolderOption> = emptyList(),
        val draftsSpecialFolderOptions: List<SpecialFolderOption> = emptyList(),
        val sentSpecialFolderOptions: List<SpecialFolderOption> = emptyList(),
        val spamSpecialFolderOptions: List<SpecialFolderOption> = emptyList(),
        val trashSpecialFolderOptions: List<SpecialFolderOption> = emptyList(),

        val selectedArchiveSpecialFolderOption: SpecialFolderOption = SpecialFolderOption.None(true),
        val selectedDraftsSpecialFolderOption: SpecialFolderOption = SpecialFolderOption.None(true),
        val selectedSentSpecialFolderOption: SpecialFolderOption = SpecialFolderOption.None(true),
        val selectedSpamSpecialFolderOption: SpecialFolderOption = SpecialFolderOption.None(true),
        val selectedTrashSpecialFolderOption: SpecialFolderOption = SpecialFolderOption.None(true),
    )

    sealed interface Event {
        data object LoadSpecialFolderOptions : Event
        data object OnRetryClicked : Event
        data object OnNextClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface FormEvent : Event {
        data class ArchiveFolderChanged(val specialFolderOption: SpecialFolderOption) : FormEvent
        data class DraftsFolderChanged(val specialFolderOption: SpecialFolderOption) : FormEvent
        data class SentFolderChanged(val specialFolderOption: SpecialFolderOption) : FormEvent
        data class SpamFolderChanged(val specialFolderOption: SpecialFolderOption) : FormEvent
        data class TrashFolderChanged(val specialFolderOption: SpecialFolderOption) : FormEvent
    }

    sealed interface Effect {
        data class NavigateNext(
            val isManualSetup: Boolean,
        ) : Effect

        data object NavigateBack : Effect
    }

    sealed interface Failure {
        data class LoadFoldersFailed(val messageFromServer: String?) : Failure
    }
}
