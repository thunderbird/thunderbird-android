package app.k9mail.feature.account.edit.ui.server.settings.save

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.ui.WizardConstants
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Effect
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Event
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Failure
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.State
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.ViewModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseSaveServerSettingsViewModel(
    val accountUuid: String,
    override val isIncoming: Boolean,
    private val saveServerSettings: AccountEditDomainContract.UseCase.SaveServerSettings,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.SaveServerSettings -> handleOneTimeEvent(event, ::onSaveServerSettings)
            Event.OnBackClicked -> navigateBack()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onSaveServerSettings() {
        viewModelScope.launch {
            try {
                saveServerSettings.execute(accountUuid, isIncoming)
                updateSuccess()
            } catch (e: Exception) {
                updateFailure(Failure.SaveServerSettingsFailed(e.message ?: "Unknown error"))
            }
        }
    }

    private fun updateSuccess() {
        updateState {
            it.copy(
                isLoading = false,
            )
        }

        viewModelScope.launch {
            delay(WizardConstants.CONTINUE_NEXT_DELAY)
            navigateNext()
        }
    }

    private fun updateFailure(failure: Failure) {
        updateState {
            it.copy(
                error = failure,
                isLoading = false,
            )
        }
    }

    private fun navigateNext() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateNext)
    }

    private fun navigateBack() {
        if (state.value.isLoading || state.value.error == null) return

        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateBack)
    }
}
