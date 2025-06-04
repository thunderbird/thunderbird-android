package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import app.k9mail.feature.account.common.ui.WizardConstants
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Effect
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormEvent
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.State
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.ViewModel
import com.fsck.k9.mail.folders.FolderFetcherException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import net.thunderbird.core.logging.legacy.Log

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
                            isManualSetup = true,
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

                    delay(WizardConstants.CONTINUE_NEXT_DELAY)
                    navigateNext()
                }
            }
        }
    }

    private suspend fun loadSpecialFolderOptions(): SpecialFolderOptions? {
        return try {
            getSpecialFolderOptions()
        } catch (exception: FolderFetcherException) {
            Log.e(exception, "Error while loading special folders")
            updateState { state ->
                state.copy(
                    isLoading = false,
                    isSuccess = false,
                    error = SpecialFoldersContract.Failure.LoadFoldersFailed(exception.messageFromServer),
                )
            }
            null
        }
    }

    private fun saveSpecialFolderSettings() {
        val formState = state.value.formState

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
    }

    private fun onNextClicked() {
        saveSpecialFolderSettings()
        navigateNext()
    }

    private fun navigateNext() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateNext(isManualSetup = state.value.isManualSetup))
    }

    private fun onBackClicked() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateBack)
    }

    private fun onRetryClicked() {
        viewModelScope.coroutineContext.cancelChildren()
        updateState {
            it.copy(
                isLoading = true,
                error = null,
            )
        }
        onLoadSpecialFolderOptions()
    }
}
