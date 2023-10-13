package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.lifecycle.viewModelScope
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Effect
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Failure.SaveFailed
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormEvent
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.State
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.ViewModel
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.folders.FolderFetcherException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CONTINUE_NEXT_DELAY = 1500L

class SpecialFoldersViewModel(
    private val formUiModel: SpecialFoldersContract.FormUiModel,
    private val getSpecialFolderOptions: UseCase.GetSpecialFolderOptions,
    private val validateSpecialFolderOptions: UseCase.ValidateSpecialFolderOptions,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadSpecialFolderOptions -> handleOneTimeEvent(event, ::onLoadSpecialFolderOptions)

            is FormEvent -> onFormEvent(event)

            Event.OnNextClicked -> onNextClicked()
            Event.OnBackClicked -> onBackClicked()
            Event.OnEditClicked -> onEditClicked()
            Event.OnRetryClicked -> onRetryClicked()
        }
    }

    private fun onFormEvent(event: FormEvent) {
        updateState {
            it.copy(
                formState = formUiModel.event(event, it.formState),
            )
        }
    }

    private fun onLoadSpecialFolderOptions() {
        viewModelScope.launch {
            val specialFolderOptions = loadSpecialFolderOptions() ?: return@launch

            updateState { state ->
                state.copy(
                    formState = specialFolderOptions.toFormState(),
                )
            }

            val result = validateSpecialFolderOptions(specialFolderOptions)
            when (result) {
                is ValidationResult.Failure -> {
                    updateState {
                        it.copy(
                            isSuccess = false,
                            isLoading = false,
                        )
                    }
                }

                ValidationResult.Success -> {
                    updateState {
                        it.copy(
                            isSuccess = true,
                        )
                    }
                    saveSpecialFolderSettings()
                }
            }
        }
    }

    private suspend fun loadSpecialFolderOptions(): SpecialFolderOptions? {
        return try {
            getSpecialFolderOptions()
        } catch (exception: FolderFetcherException) {
            Timber.e(exception, "Error while loading special folders")
            updateState { state ->
                state.copy(
                    isLoading = false,
                    isSuccess = false,
                    error = SpecialFoldersContract.Failure.LoadFoldersFailed(exception.message ?: "unknown error"),
                )
            }
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun saveSpecialFolderSettings() {
        val formState = state.value.formState

        try {
            accountStateRepository.setSpecialFolderSettings(
                SpecialFolderSettings(
                    archiveSpecialFolderOption = formState.selectedArchiveSpecialFolderOption,
                    draftsSpecialFolderOption = formState.selectedDraftsSpecialFolderOption,
                    sentSpecialFolderOption = formState.selectedSentSpecialFolderOption,
                    spamSpecialFolderOption = formState.selectedSpamSpecialFolderOption,
                    trashSpecialFolderOption = formState.selectedTrashSpecialFolderOption,
                ),
            )
            updateState { state ->
                state.copy(
                    isLoading = false,
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Error while saving special folders")
            updateState { state ->
                state.copy(
                    isLoading = false,
                    error = SaveFailed(exception.message ?: "unknown error"),
                )
            }
            return
        }

        delay(CONTINUE_NEXT_DELAY)
        navigateNext()
    }

    private fun onNextClicked() {
        viewModelScope.launch {
            saveSpecialFolderSettings()
        }
    }

    private fun navigateNext() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateNext)
    }

    private fun onBackClicked() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateBack)
    }

    private fun onEditClicked() {
        viewModelScope.coroutineContext.cancelChildren()
        updateState { state ->
            state.copy(
                isSuccess = false,
            )
        }
    }

    private fun onRetryClicked() {
        viewModelScope.coroutineContext.cancelChildren()
        updateState {
            it.copy(
                error = null,
            )
        }
    }
}
