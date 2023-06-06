package app.k9mail.feature.account.setup.ui.autoconfig

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep.EMAIL_ADDRESS
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep.OAUTH
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep.PASSWORD
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Effect
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Event
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.State
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ViewModel

@Suppress("TooManyFunctions")
class AccountAutoConfigViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.EmailAddressChanged -> changeEmailAddress(event.emailAddress)
            is Event.PasswordChanged -> changePassword(event.password)
            Event.OnNextClicked -> submit()
            Event.OnBackClicked -> navigateBack()
            Event.OnRetryClicked -> retry()
        }
    }

    private fun changeEmailAddress(emailAddress: String) {
        updateState {
            State(
                emailAddress = StringInputField(value = emailAddress),
            )
        }
    }

    private fun changePassword(password: String) {
        updateState {
            it.copy(
                password = it.password.updateValue(password),
            )
        }
    }

    private fun submit() {
        when (state.value.configStep) {
            EMAIL_ADDRESS -> submitEmail()
            PASSWORD -> submitPassword()
            OAUTH -> TODO()
        }
    }

    private fun retry() {
        TODO()
    }

    private fun submitEmail() {
        navigateNext()
    }

    private fun submitPassword() {
        navigateNext()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
